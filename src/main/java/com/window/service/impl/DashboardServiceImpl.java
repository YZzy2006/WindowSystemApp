package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.*;
import com.window.mapper.*;
import com.window.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

import java.util.*;
import com.window.common.KeywordUtil;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final PaymentMapper paymentMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final EnquiryMapper enquiryMapper;

    private LocalDate safeParseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> getOverview(String startDate, String endDate) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        // 解析日期参数，默认本月
        LocalDate periodStart = safeParseDate(startDate);
        LocalDate periodEnd = safeParseDate(endDate);
        if (periodStart == null) periodStart = thisMonth.atDay(1);
        if (periodEnd == null) periodEnd = thisMonth.atEndOfMonth();

        boolean containsToday = !today.isBefore(periodStart) && !today.isAfter(periodEnd);

        // 1. 查询区间内所有订单（排除已取消）
        LambdaQueryWrapper<SaleOrder> periodWrapper = new LambdaQueryWrapper<>();
        periodWrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        periodWrapper.ne(SaleOrder::getStatus, "cancelled");
        periodWrapper.ge(SaleOrder::getOrderDate, periodStart);
        periodWrapper.le(SaleOrder::getOrderDate, periodEnd);
        List<SaleOrder> periodOrders = saleOrderMapper.selectList(periodWrapper);

        // 区间销售额
        BigDecimal periodSales = BigDecimal.ZERO;
        BigDecimal periodReceivable = BigDecimal.ZERO;
        for (SaleOrder o : periodOrders) {
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            periodSales = periodSales.add(total);
            if (!"completed".equals(o.getStatus())) {
                periodReceivable = periodReceivable.add(total.subtract(paid).max(BigDecimal.ZERO));
            }
        }

        // 今日订单（仅当区间包含今天时）
        int todayCount = 0;
        BigDecimal todayAmount = BigDecimal.ZERO;
        if (containsToday) {
            for (SaleOrder o : periodOrders) {
                if (today.equals(o.getOrderDate())) {
                    todayCount++;
                    todayAmount = todayAmount.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
                }
            }
        }

        // 区间成本和毛利
        BigDecimal periodCost = BigDecimal.ZERO;
        if (!periodOrders.isEmpty()) {
            List<Integer> orderIds = periodOrders.stream().map(SaleOrder::getId).collect(Collectors.toList());
            LambdaQueryWrapper<SaleOrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(SaleOrderItem::getOrderId, orderIds);
            List<SaleOrderItem> items = saleOrderItemMapper.selectList(itemWrapper);
            for (SaleOrderItem item : items) {
                BigDecimal cost = item.getCost() != null ? item.getCost() : BigDecimal.ZERO;
                BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                periodCost = periodCost.add(cost.multiply(qty));
            }
        }
        BigDecimal periodProfit = periodSales.subtract(periodCost);

        // 区间收付款
        LambdaQueryWrapper<Payment> payWrapper = new LambdaQueryWrapper<>();
        payWrapper.ge(Payment::getPaymentDate, periodStart);
        payWrapper.le(Payment::getPaymentDate, periodEnd);
        List<Payment> periodPayments = paymentMapper.selectList(payWrapper);
        BigDecimal periodReceipt = BigDecimal.ZERO;
        BigDecimal periodPayment = BigDecimal.ZERO;
        for (Payment p : periodPayments) {
            BigDecimal amt = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
            if ("receipt".equals(p.getType())) {
                periodReceipt = periodReceipt.add(amt);
            } else {
                periodPayment = periodPayment.add(amt);
            }
        }

        // 2. 近12个月趋势（SQL 聚合，2 次查询代替 24 次）
        String trendStart = thisMonth.minusMonths(11).atDay(1).toString();
        String trendEnd = thisMonth.atEndOfMonth().toString();
        List<Map<String, Object>> salesTrend = saleOrderMapper.monthlyTrend(trendStart, trendEnd);
        List<Map<String, Object>> costTrend = saleOrderItemMapper.monthlyCost(trendStart, trendEnd);
        Map<Integer, BigDecimal> costMap = new HashMap<>();
        for (Map<String, Object> row : costTrend) {
            int key = ((Number) row.get("yr")).intValue() * 100 + ((Number) row.get("mo")).intValue();
            BigDecimal c = (BigDecimal) row.get("cost");
            costMap.put(key, c != null ? c : BigDecimal.ZERO);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            String ymStr = ym.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            int monthKey = ym.getYear() * 100 + ym.getMonthValue();
            BigDecimal sales = BigDecimal.ZERO;
            int count = 0;
            for (Map<String, Object> row : salesTrend) {
                int rowKey = ((Number) row.get("yr")).intValue() * 100 + ((Number) row.get("mo")).intValue();
                if (monthKey == rowKey) {
                    BigDecimal s = (BigDecimal) row.get("sales");
                    sales = s != null ? s : BigDecimal.ZERO;
                    count = ((Number) row.get("cnt")).intValue();
                    break;
                }
            }
            BigDecimal cost = costMap.getOrDefault(monthKey, BigDecimal.ZERO);
            if (cost == null) cost = BigDecimal.ZERO;
            Map<String, Object> point = new HashMap<>();
            point.put("month", ymStr);
            point.put("sales", sales);
            point.put("profit", sales.subtract(cost));
            point.put("count", count);
            trend.add(point);
        }

        // 3. 最近订单（不受参数影响）
        LambdaQueryWrapper<SaleOrder> recentWrapper = new LambdaQueryWrapper<>();
        recentWrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        recentWrapper.ne(SaleOrder::getStatus, "cancelled");
        recentWrapper.orderByDesc(SaleOrder::getOrderDate).orderByDesc(SaleOrder::getId);
        recentWrapper.last("LIMIT 5");
        List<SaleOrder> recentOrders = saleOrderMapper.selectList(recentWrapper);

        List<Map<String, Object>> recentList = new ArrayList<>();
        for (SaleOrder o : recentOrders) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("customerName", o.getCustomerName());
            m.put("totalAmount", o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
            m.put("paidAmount", o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO);
            m.put("orderDate", o.getOrderDate());
            m.put("status", o.getStatus());
            recentList.add(m);
        }

        // 4. 订单状态分布（SQL GROUP BY，1 次查询代替加载全部订单）
        List<Map<String, Object>> statusRows = saleOrderMapper.countByStatus(periodStart.toString(), periodEnd.toString());
        Map<String, Long> statusDist = new HashMap<>();
        for (Map<String, Object> row : statusRows) {
            String status = row.get("status") != null ? (String) row.get("status") : "unknown";
            long cnt = ((Number) row.get("cnt")).longValue();
            statusDist.put(status, cnt);
        }

        // 组装结果
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> todayMap = new HashMap<>();
        todayMap.put("count", containsToday ? todayCount : periodOrders.size());
        todayMap.put("amount", containsToday ? todayAmount : periodSales);
        todayMap.put("isToday", containsToday);
        result.put("today", todayMap);

        Map<String, Object> monthMap = new HashMap<>();
        monthMap.put("salesAmount", periodSales);
        monthMap.put("profit", periodProfit);
        monthMap.put("receivable", periodReceivable);
        monthMap.put("receipt", periodReceipt);
        monthMap.put("payment", periodPayment);
        monthMap.put("orderCount", periodOrders.size());
        result.put("month", monthMap);

        result.put("trend", trend);
        result.put("recentOrders", recentList);
        result.put("statusDistribution", statusDist);

        // 5. 上一时段数据（环比）— 自动计算同等长度的前一时段
        long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
        LocalDate compEnd = periodStart.minusDays(1);
        LocalDate compStart = compEnd.minusDays(periodDays - 1);

        LambdaQueryWrapper<SaleOrder> lw = new LambdaQueryWrapper<>();
        lw.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        lw.ne(SaleOrder::getStatus, "cancelled");
        lw.ge(SaleOrder::getOrderDate, compStart);
        lw.le(SaleOrder::getOrderDate, compEnd);
        List<SaleOrder> lastOrders = saleOrderMapper.selectList(lw);
        BigDecimal lastSales = BigDecimal.ZERO;
        BigDecimal lastCost = BigDecimal.ZERO;
        BigDecimal lastReceivable = BigDecimal.ZERO;
        for (SaleOrder o : lastOrders) {
            BigDecimal t = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal p = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            lastSales = lastSales.add(t);
            if (!"completed".equals(o.getStatus())) {
                lastReceivable = lastReceivable.add(t.subtract(p).max(BigDecimal.ZERO));
            }
        }
        if (!lastOrders.isEmpty()) {
            List<Integer> lastIds = lastOrders.stream().map(SaleOrder::getId).collect(Collectors.toList());
            LambdaQueryWrapper<SaleOrderItem> liw = new LambdaQueryWrapper<>();
            liw.in(SaleOrderItem::getOrderId, lastIds);
            List<SaleOrderItem> lastItems = saleOrderItemMapper.selectList(liw);
            for (SaleOrderItem item : lastItems) {
                BigDecimal c = item.getCost() != null ? item.getCost() : BigDecimal.ZERO;
                BigDecimal q = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                lastCost = lastCost.add(c.multiply(q));
            }
        }
        Map<String, Object> lastMonthMap = new HashMap<>();
        lastMonthMap.put("salesAmount", lastSales);
        lastMonthMap.put("profit", lastSales.subtract(lastCost));
        lastMonthMap.put("receivable", lastReceivable);
        lastMonthMap.put("orderCount", lastOrders.size());
        result.put("lastMonth", lastMonthMap);

        // 6. 今日待办（始终实时，不受参数影响）
        LambdaQueryWrapper<SaleOrder> todaySaleW = new LambdaQueryWrapper<>();
        todaySaleW.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        todaySaleW.ne(SaleOrder::getStatus, "cancelled");
        todaySaleW.eq(SaleOrder::getOrderDate, today);
        List<SaleOrder> todaySaleOrders = saleOrderMapper.selectList(todaySaleW);
        long todayPendingSale = todaySaleOrders.stream()
                .filter(o -> "pending".equals(o.getStatus())).count();

        LambdaQueryWrapper<PurchaseOrder> todayPoW = new LambdaQueryWrapper<>();
        todayPoW.ne(PurchaseOrder::getStatus, "cancelled");
        todayPoW.eq(PurchaseOrder::getOrderDate, today);
        long todayPurchaseCount = purchaseOrderMapper.selectCount(todayPoW);

        LambdaQueryWrapper<Enquiry> todayEnqW = new LambdaQueryWrapper<>();
        todayEnqW.eq(Enquiry::getIsRead, 0);
        long unreadEnquiries = enquiryMapper.selectCount(todayEnqW);

        Map<String, Object> todayPendingMap = new HashMap<>();
        todayPendingMap.put("todayOrderCount", todaySaleOrders.size());
        todayPendingMap.put("pendingSaleCount", todayPendingSale);
        todayPendingMap.put("purchaseOrderCount", todayPurchaseCount);
        todayPendingMap.put("unreadEnquiries", unreadEnquiries);
        result.put("todayPending", todayPendingMap);

        return result;
    }

    @Override
    public List<Map<String, Object>> getCustomerRank(String startDate, String endDate) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        wrapper.ne(SaleOrder::getStatus, "cancelled");
        LocalDate start = safeParseDate(startDate);
        LocalDate end = safeParseDate(endDate);
        if (start != null) wrapper.ge(SaleOrder::getOrderDate, start);
        if (end != null) wrapper.le(SaleOrder::getOrderDate, end);
        List<SaleOrder> orders = saleOrderMapper.selectList(wrapper);

        Map<String, List<SaleOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCustomerName() != null ? o.getCustomerName() : "未知"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<SaleOrder>> entry : grouped.entrySet()) {
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal unpaid = BigDecimal.ZERO;
            for (SaleOrder o : entry.getValue()) {
                BigDecimal t = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal p = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
                total = total.add(t);
                unpaid = unpaid.add(t.subtract(p).max(BigDecimal.ZERO));
            }
            Map<String, Object> m = new HashMap<>();
            m.put("customerName", entry.getKey());
            m.put("totalAmount", total);
            m.put("orderCount", entry.getValue().size());
            m.put("unpaidAmount", unpaid);
            result.add(m);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));
        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    @Override
    public List<Map<String, Object>> getProductRank(String startDate, String endDate) {
        LambdaQueryWrapper<SaleOrder> ow = new LambdaQueryWrapper<>();
        ow.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        ow.ne(SaleOrder::getStatus, "cancelled");
        LocalDate start = safeParseDate(startDate);
        LocalDate end = safeParseDate(endDate);
        if (start != null) ow.ge(SaleOrder::getOrderDate, start);
        if (end != null) ow.le(SaleOrder::getOrderDate, end);
        List<SaleOrder> orders = saleOrderMapper.selectList(ow);
        if (orders.isEmpty()) return Collections.emptyList();

        List<Integer> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());
        LambdaQueryWrapper<SaleOrderItem> iw = new LambdaQueryWrapper<>();
        iw.in(SaleOrderItem::getOrderId, orderIds);
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(iw);

        Map<String, List<SaleOrderItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(i -> i.getProductName() != null ? i.getProductName() : "未知"));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<SaleOrderItem>> entry : grouped.entrySet()) {
            BigDecimal qty = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
            for (SaleOrderItem item : entry.getValue()) {
                BigDecimal q = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal p = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                qty = qty.add(q);
                amount = amount.add(q.multiply(p));
            }
            Map<String, Object> m = new HashMap<>();
            m.put("productName", entry.getKey());
            m.put("totalQuantity", qty);
            m.put("totalAmount", amount);
            result.add(m);
        }
        result.sort((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")));
        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    @Override
    public List<Map<String, Object>> getReceivableAging(String startDate, String endDate) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        wrapper.ne(SaleOrder::getStatus, "cancelled");
        wrapper.ne(SaleOrder::getStatus, "completed");
        wrapper.gt(SaleOrder::getTotalAmount, 0);
        LocalDate start = safeParseDate(startDate);
        LocalDate end = safeParseDate(endDate);
        if (start != null) wrapper.ge(SaleOrder::getOrderDate, start);
        if (end != null) wrapper.le(SaleOrder::getOrderDate, end);
        List<SaleOrder> orders = saleOrderMapper.selectList(wrapper);

        LocalDate today = LocalDate.now();
        BigDecimal b0 = BigDecimal.ZERO, b30 = BigDecimal.ZERO, b60 = BigDecimal.ZERO, b90 = BigDecimal.ZERO;
        int c0 = 0, c30 = 0, c60 = 0, c90 = 0;

        for (SaleOrder o : orders) {
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal unpaid = total.subtract(paid);
            if (unpaid.compareTo(BigDecimal.ZERO) <= 0) continue;

            long days = o.getOrderDate() != null ? ChronoUnit.DAYS.between(o.getOrderDate(), today) : 0;
            if (days <= 30) { b0 = b0.add(unpaid); c0++; }
            else if (days <= 60) { b30 = b30.add(unpaid); c30++; }
            else if (days <= 90) { b60 = b60.add(unpaid); c60++; }
            else { b90 = b90.add(unpaid); c90++; }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        String[] labels = {"0-30天", "30-60天", "60-90天", "90天以上"};
        BigDecimal[] amounts = {b0, b30, b60, b90};
        int[] counts = {c0, c30, c60, c90};
        for (int i = 0; i < 4; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("bucket", labels[i]);
            m.put("amount", amounts[i]);
            m.put("count", counts[i]);
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> getReceivableAgingDetails(String startDate, String endDate, int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        if (size > 10000) size = 10000;
        int total = saleOrderMapper.countReceivableAgingDetails(startDate, endDate);
        int offset = (page - 1) * size;
        List<Map<String, Object>> list = saleOrderMapper.selectReceivableAgingDetails(startDate, endDate, offset, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        return result;
    }

    @Override
    public Map<String, Object> getCustomerOrders(String keyword, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        boolean hasKw = keyword != null && !keyword.trim().isEmpty();
        String kw = hasKw ? keyword.trim() : null;

        // 第一步：按日期+关键字筛选出活跃客户名单
        LambdaQueryWrapper<SaleOrder> nameWrapper = new LambdaQueryWrapper<>();
        nameWrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        nameWrapper.ne(SaleOrder::getStatus, "cancelled");
        nameWrapper.ne(SaleOrder::getStatus, "completed");
        if (hasKw) {
            final String escaped = KeywordUtil.escapeLike(kw);
            nameWrapper.and(w -> w.like(SaleOrder::getCustomerName, escaped)
                    .or().like(SaleOrder::getCustomerPhone, escaped));
        }
        LocalDate start = safeParseDate(startDate);
        LocalDate end = safeParseDate(endDate);
        if (start != null) nameWrapper.ge(SaleOrder::getOrderDate, start);
        if (end != null) nameWrapper.le(SaleOrder::getOrderDate, end);
        nameWrapper.select(SaleOrder::getCustomerName);
        nameWrapper.groupBy(SaleOrder::getCustomerName);
        nameWrapper.last("LIMIT 10001");
        List<SaleOrder> matched = saleOrderMapper.selectList(nameWrapper);
        boolean customerTruncated = matched.size() > 10000;
        if (customerTruncated) matched = matched.subList(0, 10000);
        Set<String> customerNames = matched.stream()
                .map(SaleOrder::getCustomerName)
                .filter(n -> n != null && !n.isEmpty())
                .collect(Collectors.toSet());

        if (customerNames.isEmpty()) {
            result.put("customers", Collections.emptyList());
            result.put("total", buildTotalSummary(Collections.emptyList()));
            return result;
        }

        // 第二步：查出这些客户的全部订单（不限日期，保证账目完整）
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        wrapper.ne(SaleOrder::getStatus, "cancelled");
        wrapper.ne(SaleOrder::getStatus, "completed");
        wrapper.in(SaleOrder::getCustomerName, customerNames);
        wrapper.orderByDesc(SaleOrder::getOrderDate);
        wrapper.last("LIMIT 10001");
        List<SaleOrder> orders = saleOrderMapper.selectList(wrapper);
        boolean orderTruncated = orders.size() > 10000;
        if (orderTruncated) orders = orders.subList(0, 10000);

        LocalDate today = LocalDate.now();

        // 按客户分组
        Map<String, List<SaleOrder>> grouped = new LinkedHashMap<>();
        for (SaleOrder o : orders) {
            String name = o.getCustomerName() != null ? o.getCustomerName() : "未知";
            grouped.computeIfAbsent(name, k -> new ArrayList<>()).add(o);
        }

        List<Map<String, Object>> customers = new ArrayList<>();
        for (Map.Entry<String, List<SaleOrder>> entry : grouped.entrySet()) {
            BigDecimal cTotal = BigDecimal.ZERO, cPaid = BigDecimal.ZERO, cUnpaid = BigDecimal.ZERO;
            int unpaidCount = 0;
            List<Map<String, Object>> orderList = new ArrayList<>();

            for (SaleOrder o : entry.getValue()) {
                BigDecimal t = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal p = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
                BigDecimal u = t.subtract(p);
                long days = o.getOrderDate() != null ? ChronoUnit.DAYS.between(o.getOrderDate(), today) : 0;

                cTotal = cTotal.add(t);
                cPaid = cPaid.add(p);
                if (u.compareTo(BigDecimal.ZERO) > 0) {
                    cUnpaid = cUnpaid.add(u);
                    unpaidCount++;
                }

                String bucket;
                if (u.compareTo(BigDecimal.ZERO) <= 0) bucket = "已结清";
                else if (days <= 30) bucket = "0-30天";
                else if (days <= 60) bucket = "30-60天";
                else if (days <= 90) bucket = "60-90天";
                else bucket = "90天以上";

                Map<String, Object> m = new HashMap<>();
                m.put("id", o.getId());
                m.put("orderNo", o.getOrderNo());
                m.put("orderDate", o.getOrderDate());
                m.put("totalAmount", t);
                m.put("paidAmount", p);
                m.put("unpaidAmount", u);
                m.put("agingDays", days);
                m.put("bucket", bucket);
                orderList.add(m);
            }

            Map<String, Object> customer = new HashMap<>();
            customer.put("customerName", entry.getKey());
            customer.put("customerPhone", entry.getValue().get(0).getCustomerPhone());
            customer.put("orderCount", entry.getValue().size());
            customer.put("unpaidCount", unpaidCount);
            customer.put("totalAmount", cTotal);
            customer.put("paidAmount", cPaid);
            customer.put("unpaidAmount", cUnpaid);
            customer.put("orders", orderList);
            customers.add(customer);
        }

        result.put("customers", customers);
        result.put("total", buildTotalSummary(customers));
        if (customerTruncated) result.put("customerTruncated", true);
        if (orderTruncated) result.put("orderTruncated", true);
        return result;
    }

    private Map<String, Object> buildTotalSummary(List<Map<String, Object>> customers) {
        BigDecimal totalAmount = BigDecimal.ZERO, paidAmount = BigDecimal.ZERO, unpaidAmount = BigDecimal.ZERO;
        int orderCount = 0, unpaidCount = 0;
        for (Map<String, Object> c : customers) {
            Object ta = c.get("totalAmount");
            Object pa = c.get("paidAmount");
            Object ua = c.get("unpaidAmount");
            Object oc = c.get("orderCount");
            Object uc = c.get("unpaidCount");
            totalAmount = totalAmount.add(ta instanceof BigDecimal ? (BigDecimal) ta : BigDecimal.ZERO);
            paidAmount = paidAmount.add(pa instanceof BigDecimal ? (BigDecimal) pa : BigDecimal.ZERO);
            unpaidAmount = unpaidAmount.add(ua instanceof BigDecimal ? (BigDecimal) ua : BigDecimal.ZERO);
            orderCount += oc instanceof Number ? ((Number) oc).intValue() : 0;
            unpaidCount += uc instanceof Number ? ((Number) uc).intValue() : 0;
        }
        Map<String, Object> summary = new HashMap<>();
        summary.put("customerCount", customers.size());
        summary.put("orderCount", orderCount);
        summary.put("unpaidCount", unpaidCount);
        summary.put("totalAmount", totalAmount);
        summary.put("paidAmount", paidAmount);
        summary.put("unpaidAmount", unpaidAmount);
        return summary;
    }

    @Override
    public Map<String, Object> getReminders() {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new HashMap<>();

        // 未读询价
        LambdaQueryWrapper<Enquiry> enqW = new LambdaQueryWrapper<>();
        enqW.eq(Enquiry::getIsRead, 0);
        enqW.orderByDesc(Enquiry::getCreateTime);
        enqW.last("LIMIT 3");
        List<Enquiry> unreadEnqs = enquiryMapper.selectList(enqW);
        LambdaQueryWrapper<Enquiry> enqCountW = new LambdaQueryWrapper<>();
        enqCountW.eq(Enquiry::getIsRead, 0);
        long unreadEnqCount = enquiryMapper.selectCount(enqCountW);
        List<Map<String, Object>> enqDetails = new ArrayList<>();
        for (Enquiry e : unreadEnqs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", e.getId());
            m.put("name", e.getName());
            m.put("phone", e.getPhone());
            m.put("createTime", e.getCreateTime());
            enqDetails.add(m);
        }
        Map<String, Object> enqMap = new HashMap<>();
        enqMap.put("count", unreadEnqCount);
        enqMap.put("items", enqDetails);
        result.put("unreadEnquiries", enqMap);

        // 待处理销售单
        LambdaQueryWrapper<SaleOrder> soW = new LambdaQueryWrapper<>();
        soW.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        soW.eq(SaleOrder::getStatus, "pending");
        soW.orderByDesc(SaleOrder::getOrderDate);
        soW.last("LIMIT 3");
        List<SaleOrder> pendingSOs = saleOrderMapper.selectList(soW);
        LambdaQueryWrapper<SaleOrder> soCountW = new LambdaQueryWrapper<>();
        soCountW.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        soCountW.eq(SaleOrder::getStatus, "pending");
        long pendingSOCount = saleOrderMapper.selectCount(soCountW);
        List<Map<String, Object>> soDetails = new ArrayList<>();
        for (SaleOrder o : pendingSOs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("customerName", o.getCustomerName());
            m.put("orderDate", o.getOrderDate());
            soDetails.add(m);
        }
        Map<String, Object> soMap = new HashMap<>();
        soMap.put("count", pendingSOCount);
        soMap.put("items", soDetails);
        result.put("pendingSaleOrders", soMap);

        // 待处理采购单
        LambdaQueryWrapper<PurchaseOrder> poW = new LambdaQueryWrapper<>();
        poW.eq(PurchaseOrder::getStatus, "pending");
        poW.orderByDesc(PurchaseOrder::getOrderDate);
        poW.last("LIMIT 3");
        List<PurchaseOrder> pendingPOs = purchaseOrderMapper.selectList(poW);
        LambdaQueryWrapper<PurchaseOrder> poCountW = new LambdaQueryWrapper<>();
        poCountW.eq(PurchaseOrder::getStatus, "pending");
        long pendingPOCount = purchaseOrderMapper.selectCount(poCountW);
        List<Map<String, Object>> poDetails = new ArrayList<>();
        for (PurchaseOrder o : pendingPOs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("supplierName", o.getSupplierName());
            m.put("orderDate", o.getOrderDate());
            poDetails.add(m);
        }
        Map<String, Object> poMap = new HashMap<>();
        poMap.put("count", pendingPOCount);
        poMap.put("items", poDetails);
        result.put("pendingPurchaseOrders", poMap);

        // 逾期应收（超60天未结清）
        LocalDate cutoff = today.minusDays(60);
        LambdaQueryWrapper<SaleOrder> overdueW = new LambdaQueryWrapper<>();
        overdueW.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        overdueW.ne(SaleOrder::getStatus, "cancelled");
        overdueW.ne(SaleOrder::getStatus, "completed");
        overdueW.le(SaleOrder::getOrderDate, cutoff);
        overdueW.apply("(total_amount - COALESCE(paid_amount, 0)) > 0");
        overdueW.orderByDesc(SaleOrder::getTotalAmount);
        overdueW.last("LIMIT 3");
        List<SaleOrder> topOverdue = saleOrderMapper.selectList(overdueW);

        // 统计全部逾期数量（不限 LIMIT）
        LambdaQueryWrapper<SaleOrder> overdueCountW = new LambdaQueryWrapper<>();
        overdueCountW.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        overdueCountW.ne(SaleOrder::getStatus, "cancelled");
        overdueCountW.ne(SaleOrder::getStatus, "completed");
        overdueCountW.le(SaleOrder::getOrderDate, cutoff);
        overdueCountW.apply("(total_amount - COALESCE(paid_amount, 0)) > 0");
        long overdueCount = saleOrderMapper.selectCount(overdueCountW);

        List<Map<String, Object>> overdueDetails = new ArrayList<>();
        for (SaleOrder o : topOverdue) {
            BigDecimal total = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paid = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal unpaid = total.subtract(paid);
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("customerName", o.getCustomerName());
            m.put("unpaidAmount", unpaid);
            m.put("orderDate", o.getOrderDate());
            overdueDetails.add(m);
        }
        Map<String, Object> overdueMap = new HashMap<>();
        overdueMap.put("count", overdueCount);
        overdueMap.put("items", overdueDetails);
        result.put("overdueReceivables", overdueMap);

        return result;
    }
}
