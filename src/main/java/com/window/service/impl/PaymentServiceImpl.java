package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.*;
import com.window.entity.SaleOrderItem;
import com.window.mapper.*;
import com.window.mapper.SaleOrderItemMapper;
import com.window.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentMapper paymentMapper;
    private final SaleOrderMapper saleOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SaleReturnMapper saleReturnMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;

    @Override
    public IPage<Payment> list(Page<Payment> page, Integer orderId, String type, String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (orderId != null) {
            wrapper.eq(Payment::getOrderId, orderId);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Payment::getType, type);
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Payment::getOrderNo, escaped)
                    .or().like(Payment::getPartyName, escaped)
                    .or().like(Payment::getRemark, escaped));
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(Payment::getPaymentDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(Payment::getPaymentDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(Payment::getPaymentDate).orderByDesc(Payment::getCreateTime);
        IPage<Payment> result = paymentMapper.selectPage(page, wrapper);
        // Enrich with order cleared status（用 orderType:orderId 复合键避免跨表 ID 碰撞）
        Map<String, Integer> clearedMap = new HashMap<>();
        Map<String, Set<Integer>> idsByType = new HashMap<>();
        for (Payment p : result.getRecords()) {
            if (p.getOrderId() != null && p.getOrderType() != null) {
                idsByType.computeIfAbsent(p.getOrderType(), k -> new HashSet<>()).add(p.getOrderId());
            }
        }
        for (Map.Entry<String, Set<Integer>> entry : idsByType.entrySet()) {
            String orderType = entry.getKey();
            Set<Integer> ids = entry.getValue();
            if ("purchase".equals(orderType)) {
                LambdaQueryWrapper<PurchaseOrder> ow = new LambdaQueryWrapper<>();
                ow.select(PurchaseOrder::getId, PurchaseOrder::getIsCleared).in(PurchaseOrder::getId, ids);
                for (PurchaseOrder o : purchaseOrderMapper.selectList(ow)) {
                    clearedMap.put(orderType + ":" + o.getId(), o.getIsCleared() != null ? o.getIsCleared() : 0);
                }
            } else if ("sale_return".equals(orderType)) {
                LambdaQueryWrapper<SaleReturn> ow = new LambdaQueryWrapper<>();
                ow.select(SaleReturn::getId, SaleReturn::getIsCleared).in(SaleReturn::getId, ids);
                for (SaleReturn o : saleReturnMapper.selectList(ow)) {
                    clearedMap.put(orderType + ":" + o.getId(), o.getIsCleared() != null ? o.getIsCleared() : 0);
                }
            } else if ("purchase_return".equals(orderType)) {
                LambdaQueryWrapper<PurchaseReturn> ow = new LambdaQueryWrapper<>();
                ow.select(PurchaseReturn::getId, PurchaseReturn::getIsCleared).in(PurchaseReturn::getId, ids);
                for (PurchaseReturn o : purchaseReturnMapper.selectList(ow)) {
                    clearedMap.put(orderType + ":" + o.getId(), o.getIsCleared() != null ? o.getIsCleared() : 0);
                }
            } else {
                LambdaQueryWrapper<SaleOrder> ow = new LambdaQueryWrapper<>();
                ow.select(SaleOrder::getId, SaleOrder::getIsCleared).in(SaleOrder::getId, ids);
                for (SaleOrder o : saleOrderMapper.selectList(ow)) {
                    clearedMap.put(orderType + ":" + o.getId(), o.getIsCleared() != null ? o.getIsCleared() : 0);
                }
            }
        }
        for (Payment p : result.getRecords()) {
            if (p.getOrderId() != null && p.getOrderType() != null) {
                String key = p.getOrderType() + ":" + p.getOrderId();
                p.setOrderCleared(clearedMap.getOrDefault(key, 0));
            }
        }
        return result;
    }

    @Override
    public Payment getById(Integer id) {
        return paymentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Payment payment) {
        checkOrderNotCancelled(payment);
        checkNoOverpayment(payment);
        paymentMapper.insert(payment);
        syncOrderPaidAmount(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Payment payment) {
        Payment old = paymentMapper.selectById(payment.getId());
        if (old == null) return;
        // 订单变更时检查新订单，否则检查旧订单
        boolean orderChanged = !Objects.equals(payment.getOrderId(), old.getOrderId());
        checkOrderNotCancelled(orderChanged ? payment : old);
        // 超额校验：金额变更或订单变更时
        if (orderChanged) {
            checkNoOverpayment(payment);
        } else if (!Objects.equals(payment.getAmount(), old.getAmount())) {
            checkNoOverpayment(payment, old);
        }
        paymentMapper.updateById(payment);

        Integer oldOrderId = old.getOrderId();
        Integer newOrderId = payment.getOrderId();

        if (orderChanged && oldOrderId != null && newOrderId != null) {
            // 两个订单都需要同步时，按 orderId 升序加锁，防止并发死锁
            if (oldOrderId < newOrderId) {
                syncOrderPaidAmount(old);
                syncOrderPaidAmount(payment);
            } else {
                syncOrderPaidAmount(payment);
                syncOrderPaidAmount(old);
            }
        } else if (orderChanged) {
            // 从无订单变为有订单，或从有订单变为无订单
            syncOrderPaidAmount(old);
            syncOrderPaidAmount(payment);
        } else if (oldOrderId != null) {
            syncOrderPaidAmount(old);
        } else if (newOrderId != null) {
            syncOrderPaidAmount(payment);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        Payment payment = paymentMapper.selectById(id);
        paymentMapper.deleteById(id);
        if (payment != null) {
            syncOrderPaidAmount(payment);
        }
    }

    @Override
    public Map<String, Object> getSummary(String startDate, String endDate, String type, String keyword) {
        String escapedKeyword = StringUtils.hasText(keyword) ? KeywordUtil.escapeLike(keyword) : keyword;
        BigDecimal openingBalance = BigDecimal.ZERO;
        BigDecimal totalReceipt;
        BigDecimal totalPayment;

        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            Map<String, Object> opening = paymentMapper.selectSummaryBefore(startDate, type, escapedKeyword);
            BigDecimal openReceipt = toBD(opening.get("totalReceipt"));
            BigDecimal openPayment = toBD(opening.get("totalPayment"));
            openingBalance = openReceipt.subtract(openPayment);
            Map<String, Object> period = paymentMapper.selectSummary(startDate, endDate, type, escapedKeyword);
            totalReceipt = toBD(period.get("totalReceipt"));
            totalPayment = toBD(period.get("totalPayment"));
        } else if (StringUtils.hasText(startDate)) {
            Map<String, Object> data = paymentMapper.selectSummaryFrom(startDate, type, escapedKeyword);
            totalReceipt = toBD(data.get("totalReceipt"));
            totalPayment = toBD(data.get("totalPayment"));
        } else if (StringUtils.hasText(endDate)) {
            Map<String, Object> data = paymentMapper.selectSummaryUntil(endDate, type, escapedKeyword);
            totalReceipt = toBD(data.get("totalReceipt"));
            totalPayment = toBD(data.get("totalPayment"));
        } else {
            Map<String, Object> all = paymentMapper.selectAllSummary(type, escapedKeyword);
            totalReceipt = toBD(all.get("totalReceipt"));
            totalPayment = toBD(all.get("totalPayment"));
        }
        BigDecimal net = totalReceipt.subtract(totalPayment);

        Map<String, Object> summary = new HashMap<>();
        summary.put("openingBalance", openingBalance);
        summary.put("totalReceipt", totalReceipt);
        summary.put("totalPayment", totalPayment);
        summary.put("net", net);
        summary.put("closingBalance", openingBalance.add(net));
        return summary;
    }

    @Override
    public List<Map<String, Object>> getUnpaidOrders(String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();

        // 销售订单（含预算单），排除已取消
        LambdaQueryWrapper<SaleOrder> sw = new LambdaQueryWrapper<>();
        sw.eq(SaleOrder::getIsCleared, 0).ne(SaleOrder::getStatus, "cancelled").gt(SaleOrder::getTotalAmount, 0);
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            if (keyword.matches("\\d+")) {
                Integer kid = Integer.parseInt(keyword);
                sw.and(w -> w.like(SaleOrder::getOrderNo, escaped).or().eq(SaleOrder::getId, kid));
            } else {
                sw.and(w -> w.like(SaleOrder::getOrderNo, escaped).or().like(SaleOrder::getCustomerName, escaped));
            }
        }
        for (SaleOrder o : saleOrderMapper.selectList(sw)) {
            result.add(buildUnpaidMap(o.getId(), o.getOrderNo(), o.getOrderType(),
                    o.getCustomerName(), o.getTotalAmount(), o.getPaidAmount(), o.getDeposit()));
        }

        // 采购订单，排除已取消
        LambdaQueryWrapper<PurchaseOrder> pw = new LambdaQueryWrapper<>();
        pw.eq(PurchaseOrder::getIsCleared, 0).ne(PurchaseOrder::getStatus, "cancelled").gt(PurchaseOrder::getTotalAmount, 0);
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            if (keyword.matches("\\d+")) {
                Integer kid = Integer.parseInt(keyword);
                pw.and(w -> w.like(PurchaseOrder::getOrderNo, escaped).or().eq(PurchaseOrder::getId, kid));
            } else {
                pw.and(w -> w.like(PurchaseOrder::getOrderNo, escaped).or().like(PurchaseOrder::getSupplierName, escaped));
            }
        }
        for (PurchaseOrder o : purchaseOrderMapper.selectList(pw)) {
            result.add(buildUnpaidMap(o.getId(), o.getOrderNo(), "purchase",
                    o.getSupplierName(), o.getTotalAmount(), o.getPaidAmount(), null));
        }

        // 销售退货，排除已取消
        LambdaQueryWrapper<SaleReturn> srw = new LambdaQueryWrapper<>();
        srw.eq(SaleReturn::getIsCleared, 0).ne(SaleReturn::getStatus, "cancelled").gt(SaleReturn::getTotalAmount, 0);
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            if (keyword.matches("\\d+")) {
                Integer kid = Integer.parseInt(keyword);
                srw.and(w -> w.like(SaleReturn::getOrderNo, escaped).or().eq(SaleReturn::getId, kid));
            } else {
                srw.and(w -> w.like(SaleReturn::getOrderNo, escaped).or().like(SaleReturn::getCustomerName, escaped));
            }
        }
        for (SaleReturn o : saleReturnMapper.selectList(srw)) {
            result.add(buildUnpaidMap(o.getId(), o.getOrderNo(), "sale_return",
                    o.getCustomerName(), o.getTotalAmount(), o.getPaidAmount(), null));
        }

        // 采购退货，排除已取消
        LambdaQueryWrapper<PurchaseReturn> prw = new LambdaQueryWrapper<>();
        prw.eq(PurchaseReturn::getIsCleared, 0).ne(PurchaseReturn::getStatus, "cancelled").gt(PurchaseReturn::getTotalAmount, 0);
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            if (keyword.matches("\\d+")) {
                Integer kid = Integer.parseInt(keyword);
                prw.and(w -> w.like(PurchaseReturn::getOrderNo, escaped).or().eq(PurchaseReturn::getId, kid));
            } else {
                prw.and(w -> w.like(PurchaseReturn::getOrderNo, escaped).or().like(PurchaseReturn::getSupplierName, escaped));
            }
        }
        for (PurchaseReturn o : purchaseReturnMapper.selectList(prw)) {
            result.add(buildUnpaidMap(o.getId(), o.getOrderNo(), "purchase_return",
                    o.getSupplierName(), o.getTotalAmount(), o.getPaidAmount(), null));
        }

        return result;
    }

    private Map<String, Object> buildUnpaidMap(Integer orderId, String orderNo, String orderType,
                                                String partyName, BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal deposit) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId);
        map.put("orderNo", orderNo);
        map.put("orderType", orderType);
        map.put("customerName", partyName);
        map.put("totalAmount", totalAmount);
        BigDecimal paid = paidAmount != null ? paidAmount : BigDecimal.ZERO;
        map.put("paidAmount", paid);
        map.put("unpaidAmount", totalAmount.subtract(paid));
        map.put("deposit", deposit != null ? deposit : BigDecimal.ZERO);
        map.put("cost", BigDecimal.ZERO);
        map.put("profit", BigDecimal.ZERO);
        map.put("profitRate", BigDecimal.ZERO);
        return map;
    }

    private void syncOrderPaidAmount(Payment payment) {
        if (payment == null || payment.getOrderId() == null) return;
        Integer orderId = payment.getOrderId();
        String orderType = payment.getOrderType();

        // 根据订单类型决定期望的收款/付款类型
        if ("purchase".equals(orderType)) {
            syncPurchaseOrderPaid(orderId);
        } else if ("sale_return".equals(orderType)) {
            syncSaleReturnPaid(orderId);
        } else if ("purchase_return".equals(orderType)) {
            syncPurchaseReturnPaid(orderId);
        } else {
            // 默认为销售订单（sale / presale）
            syncSaleOrderPaid(orderId);
        }
    }

    private void syncSaleOrderPaid(Integer orderId) {
        // 先锁订单行，防止并发支付导致 paidAmount 丢失更新
        LambdaQueryWrapper<SaleOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleOrder::getId, orderId).last("FOR UPDATE");
        SaleOrder order = saleOrderMapper.selectOne(lockWrapper);
        if (order == null) return;

        BigDecimal totalPaid = sumPayments(orderId, "receipt");

        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        saleOrderMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(order.getStatus()) && !"cancelled".equals(order.getStatus())) {
            saleOrderMapper.updateStatus(orderId, "completed");
        }
    }

    private void syncPurchaseOrderPaid(Integer orderId) {
        LambdaQueryWrapper<PurchaseOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseOrder::getId, orderId).last("FOR UPDATE");
        PurchaseOrder order = purchaseOrderMapper.selectOne(lockWrapper);
        if (order == null) return;

        BigDecimal totalPaid = sumPayments(orderId, "payment");

        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        purchaseOrderMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(order.getStatus()) && !"cancelled".equals(order.getStatus())) {
            purchaseOrderMapper.updateStatus(orderId, "completed");
        }
    }

    private void syncSaleReturnPaid(Integer orderId) {
        LambdaQueryWrapper<SaleReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleReturn::getId, orderId).last("FOR UPDATE");
        SaleReturn order = saleReturnMapper.selectOne(lockWrapper);
        if (order == null) return;

        BigDecimal totalPaid = sumPayments(orderId, "payment");

        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        saleReturnMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(order.getStatus()) && !"cancelled".equals(order.getStatus())) {
            saleReturnMapper.updateStatus(orderId, "completed");
        }
    }

    private void syncPurchaseReturnPaid(Integer orderId) {
        LambdaQueryWrapper<PurchaseReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseReturn::getId, orderId).last("FOR UPDATE");
        PurchaseReturn order = purchaseReturnMapper.selectOne(lockWrapper);
        if (order == null) return;

        BigDecimal totalPaid = sumPayments(orderId, "receipt");

        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        purchaseReturnMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(order.getStatus()) && !"cancelled".equals(order.getStatus())) {
            purchaseReturnMapper.updateStatus(orderId, "completed");
        }
    }

    private BigDecimal sumPayments(Integer orderId, String type) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId);
        if (type != null) {
            wrapper.eq(Payment::getType, type);
        }
        List<Payment> payments = paymentMapper.selectList(wrapper);
        return payments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal toBD(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        return new BigDecimal(val.toString());
    }

    private void checkOrderNotCancelled(Payment payment) {
        if (payment.getOrderId() == null) return;
        String status = null;
        String orderType = payment.getOrderType();
        if ("purchase".equals(orderType)) {
            PurchaseOrder o = purchaseOrderMapper.selectById(payment.getOrderId());
            if (o != null) status = o.getStatus();
        } else if ("sale_return".equals(orderType)) {
            SaleReturn o = saleReturnMapper.selectById(payment.getOrderId());
            if (o != null) status = o.getStatus();
        } else if ("purchase_return".equals(orderType)) {
            PurchaseReturn o = purchaseReturnMapper.selectById(payment.getOrderId());
            if (o != null) status = o.getStatus();
        } else {
            SaleOrder o = saleOrderMapper.selectById(payment.getOrderId());
            if (o != null) status = o.getStatus();
        }
        if ("cancelled".equals(status)) {
            throw new IllegalStateException("已取消的订单不能收付款");
        }
    }

    private void checkNoOverpayment(Payment payment) {
        checkNoOverpayment(payment, null);
    }

    private void checkNoOverpayment(Payment payment, Payment oldPayment) {
        if (payment.getOrderId() == null || payment.getAmount() == null) return;
        BigDecimal totalAmount = null;
        String orderType = payment.getOrderType();
        // 先锁订单行，防止并发超额（TOCTOU 防护）
        if ("purchase".equals(orderType)) {
            LambdaQueryWrapper<PurchaseOrder> w = new LambdaQueryWrapper<>();
            w.eq(PurchaseOrder::getId, payment.getOrderId()).last("FOR UPDATE");
            PurchaseOrder o = purchaseOrderMapper.selectOne(w);
            if (o != null) totalAmount = o.getTotalAmount();
        } else if ("sale_return".equals(orderType)) {
            LambdaQueryWrapper<SaleReturn> w = new LambdaQueryWrapper<>();
            w.eq(SaleReturn::getId, payment.getOrderId()).last("FOR UPDATE");
            SaleReturn o = saleReturnMapper.selectOne(w);
            if (o != null) totalAmount = o.getTotalAmount();
        } else if ("purchase_return".equals(orderType)) {
            LambdaQueryWrapper<PurchaseReturn> w = new LambdaQueryWrapper<>();
            w.eq(PurchaseReturn::getId, payment.getOrderId()).last("FOR UPDATE");
            PurchaseReturn o = purchaseReturnMapper.selectOne(w);
            if (o != null) totalAmount = o.getTotalAmount();
        } else {
            LambdaQueryWrapper<SaleOrder> w = new LambdaQueryWrapper<>();
            w.eq(SaleOrder::getId, payment.getOrderId()).last("FOR UPDATE");
            SaleOrder o = saleOrderMapper.selectOne(w);
            if (o != null) totalAmount = o.getTotalAmount();
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) return;
        String payType = ("purchase".equals(orderType) || "sale_return".equals(orderType)) ? "payment" : "receipt";
        BigDecimal currentPaid = sumPayments(payment.getOrderId(), payType);
        // 编辑时扣除旧金额、加上新金额
        BigDecimal afterPay = currentPaid.add(payment.getAmount());
        if (oldPayment != null && oldPayment.getAmount() != null) {
            afterPay = afterPay.subtract(oldPayment.getAmount());
        }
        if (afterPay.subtract(totalAmount).compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalStateException("收付款金额超出订单总额，订单总额: " + totalAmount + "，已收付: " + currentPaid + "，本次: " + payment.getAmount());
        }
    }

    @Override
    public Map<String, Object> getPaymentReport(String startDate, String endDate, String keyword) {
        String escapedKeyword = StringUtils.hasText(keyword) ? KeywordUtil.escapeLike(keyword) : keyword;
        // 1. 查询范围内收付款列表
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(Payment::getPaymentDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(Payment::getPaymentDate, LocalDate.parse(endDate));
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Payment::getOrderNo, escaped)
                    .or().like(Payment::getPartyName, escaped)
                    .or().like(Payment::getRemark, escaped));
        }
        wrapper.orderByAsc(Payment::getPaymentDate).orderByAsc(Payment::getId);
        wrapper.last("LIMIT 1001");
        List<Payment> allPayments = paymentMapper.selectList(wrapper);
        boolean truncated = allPayments.size() > 1000;
        List<Payment> payments = truncated ? allPayments.subList(0, 1000) : allPayments;

        // 2. 本期收付款汇总（SQL 聚合，不受 1000 条截断影响）
        BigDecimal periodReceipt = BigDecimal.ZERO;
        BigDecimal periodPayment = BigDecimal.ZERO;
        Map<String, Object> periodSummary;
        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            periodSummary = paymentMapper.selectSummary(startDate, endDate, null, escapedKeyword);
        } else if (StringUtils.hasText(startDate)) {
            periodSummary = paymentMapper.selectSummaryFrom(startDate, null, escapedKeyword);
        } else if (StringUtils.hasText(endDate)) {
            periodSummary = paymentMapper.selectSummaryUntil(endDate, null, escapedKeyword);
        } else {
            periodSummary = paymentMapper.selectAllSummary(null, escapedKeyword);
        }
        periodReceipt = toBD(periodSummary.get("totalReceipt"));
        periodPayment = toBD(periodSummary.get("totalPayment"));

        // 3. 期初结存（startDate 之前的净收付款）
        BigDecimal openingBalance = BigDecimal.ZERO;
        if (StringUtils.hasText(startDate)) {
            Map<String, Object> opening = paymentMapper.selectSummaryBefore(startDate, null, escapedKeyword);
            BigDecimal openReceipt = toBD(opening.get("totalReceipt"));
            BigDecimal openPayment = toBD(opening.get("totalPayment"));
            openingBalance = openReceipt.subtract(openPayment);
        }

        BigDecimal closingBalance = openingBalance.add(periodReceipt).subtract(periodPayment);

        // 4. 构建收支明细列表
        List<Map<String, Object>> paymentDetails = new ArrayList<>();
        for (Payment p : payments) {
            Map<String, Object> pm = new HashMap<>();
            pm.put("id", p.getId());
            pm.put("paymentDate", p.getPaymentDate());
            pm.put("type", p.getType());
            pm.put("amount", p.getAmount());
            pm.put("orderNo", p.getOrderNo());
            pm.put("partyName", p.getPartyName());
            pm.put("remark", p.getRemark());
            paymentDetails.add(pm);
        }

        // 5. 组装结果
        Map<String, Object> summary = new HashMap<>();
        summary.put("openingBalance", openingBalance);
        summary.put("periodReceipt", periodReceipt);
        summary.put("periodPayment", periodPayment);
        summary.put("closingBalance", closingBalance);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("payments", paymentDetails);
        result.put("truncated", truncated);
        return result;
    }
}
