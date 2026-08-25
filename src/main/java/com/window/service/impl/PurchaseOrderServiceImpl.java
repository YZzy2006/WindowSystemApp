package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.common.StatusValidator;
import com.window.exception.OrderNoExistsException;
import com.window.entity.*;
import com.window.mapper.*;
import com.window.service.PurchaseOrderService;
import com.window.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final PaymentMapper paymentMapper;
    private final SysConfigService sysConfigService;

    @Override
    public IPage<PurchaseOrder> list(Page<PurchaseOrder> page, String keyword, String status, String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(PurchaseOrder::getOrderNo, escaped)
                    .or().like(PurchaseOrder::getSupplierName, escaped));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseOrder::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseOrder::getOrderDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(PurchaseOrder::getOrderDate).orderByDesc(PurchaseOrder::getCreateTime);
        return purchaseOrderMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getDetail(Integer id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) return null;
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getPurchaseOrderId, id);
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return result;
    }

    @Override
    public PurchaseOrder getById(Integer id) {
        return purchaseOrderMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PurchaseOrder order, List<PurchaseOrderItem> items) {
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("pending");
        }
        if (!StringUtils.hasText(order.getOrderNo())) {
            String prefix = getConfigPrefix("purchase_order_prefix", "CG");
            order.setOrderNo(generateOrderNo("purchase", prefix));
        }
        // 订单号唯一预检（含软删除行，唯一索引仍占用），导入重复订单号时给出明确提示
        if (StringUtils.hasText(order.getOrderNo()) && purchaseOrderMapper.countByOrderNo(order.getOrderNo()) > 0) {
            throw new OrderNoExistsException("订单号已存在：" + order.getOrderNo());
        }
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        order.setTotalAmount(total);
        purchaseOrderMapper.insert(order);
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                item.setPurchaseOrderId(order.getId());
                purchaseOrderItemMapper.insert(item);
            }
        }
        syncPurchaseOrderPaid(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(PurchaseOrder order, List<PurchaseOrderItem> items) {
        // 加锁防止并发编辑导致丢失更新
        LambdaQueryWrapper<PurchaseOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseOrder::getId, order.getId()).last("FOR UPDATE");
        PurchaseOrder existing = purchaseOrderMapper.selectOne(lockWrapper);
        if (existing == null) throw new IllegalArgumentException("订单不存在");
        // 内部系统：任意状态的订单（含已完成/已取消）都可编辑

        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        order.setTotalAmount(total);
        // 保留前端未传的字段，校验状态转换
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus(existing.getStatus());
        } else if (!order.getStatus().equals(existing.getStatus())) {
            StatusValidator.validateOrder(existing.getStatus(), order.getStatus());
        }
        if (items == null) {
            order.setTotalAmount(existing.getTotalAmount());
        }
        purchaseOrderMapper.updateById(order);
        if (items != null) {
            LambdaQueryWrapper<PurchaseOrderItem> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(PurchaseOrderItem::getPurchaseOrderId, order.getId());
            purchaseOrderItemMapper.delete(deleteWrapper);
            for (PurchaseOrderItem item : items) {
                item.setId(null);
                item.setPurchaseOrderId(order.getId());
                purchaseOrderItemMapper.insert(item);
            }
        }
        syncPurchaseOrderPaid(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        LambdaQueryWrapper<PurchaseOrder> lw = new LambdaQueryWrapper<>();
        lw.eq(PurchaseOrder::getId, id).last("FOR UPDATE");
        PurchaseOrder order = purchaseOrderMapper.selectOne(lw);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if (!"cancelled".equals(order.getStatus())) {
            throw new IllegalStateException("只有已取消的订单才能删除");
        }
        // 删除关联的收付款记录
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getOrderId, id);
        paymentMapper.delete(paymentWrapper);
        // 删除采购单明细
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrderItem::getPurchaseOrderId, id);
        purchaseOrderItemMapper.delete(wrapper);
        // 删除采购单
        purchaseOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, String status) {
        LambdaQueryWrapper<PurchaseOrder> lw = new LambdaQueryWrapper<>();
        lw.eq(PurchaseOrder::getId, id).last("FOR UPDATE");
        PurchaseOrder existing = purchaseOrderMapper.selectOne(lw);
        if (existing == null) throw new IllegalArgumentException("订单不存在");
        StatusValidator.validateOrder(existing.getStatus(), status);
        purchaseOrderMapper.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCleared(Integer id) {
        LambdaQueryWrapper<PurchaseOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseOrder::getId, id).last("FOR UPDATE");
        PurchaseOrder order = purchaseOrderMapper.selectOne(lockWrapper);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        if ("cancelled".equals(order.getStatus())) {
            throw new IllegalStateException("已取消的订单不能结算");
        }
        int newVal = order.getIsCleared() != null && order.getIsCleared() == 1 ? 0 : 1;
        purchaseOrderMapper.updateCleared(id, newVal);
        if (newVal == 1 && !"completed".equals(order.getStatus())) {
            purchaseOrderMapper.updateStatus(id, "completed");
        }
    }

    @Override
    public Map<String, Object> getSummary(String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(PurchaseOrder::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseOrder::getOrderDate, LocalDate.parse(endDate));
        }
        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        int totalCount = orders.size();

        // 批量查询所有付款记录，避免 N+1
        List<String> orderNos = orders.stream().map(PurchaseOrder::getOrderNo).collect(Collectors.toList());
        Map<String, BigDecimal> paidByOrderNo = new HashMap<>();
        if (!orderNos.isEmpty()) {
            LambdaQueryWrapper<Payment> pw = new LambdaQueryWrapper<>();
            pw.in(Payment::getOrderNo, orderNos).eq(Payment::getType, "payment");
            paymentMapper.selectList(pw).stream()
                    .filter(p -> p.getAmount() != null)
                    .forEach(p -> paidByOrderNo.merge(p.getOrderNo(), p.getAmount(), BigDecimal::add));
        }

        for (PurchaseOrder o : orders) {
            totalAmount = totalAmount.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
            totalPaid = totalPaid.add(paidByOrderNo.getOrDefault(o.getOrderNo(), BigDecimal.ZERO));
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("totalAmount", totalAmount);
        summary.put("totalPaid", totalPaid);
        summary.put("totalUnpaid", totalAmount.subtract(totalPaid));
        return summary;
    }

    @Override
    public Map<String, Object> getReport(String startDate, String endDate, String keyword) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(PurchaseOrder::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) wrapper.ge(PurchaseOrder::getOrderDate, LocalDate.parse(startDate));
        if (StringUtils.hasText(endDate)) wrapper.le(PurchaseOrder::getOrderDate, LocalDate.parse(endDate));
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(PurchaseOrder::getOrderNo, escaped)
                    .or().like(PurchaseOrder::getSupplierName, escaped)
                    .or().like(PurchaseOrder::getSupplierPhone, escaped)
                    .or().like(PurchaseOrder::getRemark, escaped));
        }
        wrapper.orderByAsc(PurchaseOrder::getOrderDate).orderByAsc(PurchaseOrder::getId);
        wrapper.last("LIMIT 1001");
        List<PurchaseOrder> allOrders = purchaseOrderMapper.selectList(wrapper);
        boolean truncated = allOrders.size() > 1000;
        List<PurchaseOrder> orders = truncated ? allOrders.subList(0, 1000) : allOrders;

        List<Integer> orderIds = orders.stream().map(PurchaseOrder::getId).collect(Collectors.toList());
        Map<Integer, List<Payment>> paymentsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<Payment> pw = new LambdaQueryWrapper<>();
            pw.in(Payment::getOrderId, orderIds).eq(Payment::getType, "payment");
            paymentsByOrder = paymentMapper.selectList(pw).stream()
                    .collect(Collectors.groupingBy(Payment::getOrderId));
        }

        List<Map<String, Object>> orderDetails = new ArrayList<>();
        for (PurchaseOrder o : orders) {
            BigDecimal totalAmt = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paidAmt = BigDecimal.ZERO;
            for (Payment p : paymentsByOrder.getOrDefault(o.getId(), Collections.emptyList())) {
                if (p.getAmount() != null) paidAmt = paidAmt.add(p.getAmount());
            }

            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", o.getId());
            detail.put("orderDate", o.getOrderDate());
            detail.put("orderNo", o.getOrderNo());
            detail.put("supplierName", o.getSupplierName());
            detail.put("totalAmount", totalAmt);
            detail.put("paidAmount", paidAmt);
            detail.put("unpaidAmount", totalAmt.subtract(paidAmt));

            List<Map<String, Object>> paymentList = new ArrayList<>();
            for (Payment p : paymentsByOrder.getOrDefault(o.getId(), Collections.emptyList())) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id", p.getId());
                pm.put("paymentDate", p.getPaymentDate());
                pm.put("type", p.getType());
                pm.put("amount", p.getAmount());
                pm.put("partyName", p.getPartyName());
                pm.put("remark", p.getRemark());
                paymentList.add(pm);
            }
            detail.put("payments", paymentList);
            orderDetails.add(detail);
        }

        Map<String, List<Map<String, Object>>> grouped = orderDetails.stream()
                .collect(Collectors.groupingBy(d -> (String) d.getOrDefault("supplierName", "未知")));
        List<Map<String, Object>> supplierSummary = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandPaid = BigDecimal.ZERO;

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            BigDecimal sTotal = BigDecimal.ZERO;
            BigDecimal sPaid = BigDecimal.ZERO;
            for (Map<String, Object> d : entry.getValue()) {
                sTotal = sTotal.add((BigDecimal) d.get("totalAmount"));
                sPaid = sPaid.add((BigDecimal) d.get("paidAmount"));
            }
            Map<String, Object> ss = new HashMap<>();
            ss.put("supplierName", entry.getKey());
            ss.put("totalAmount", sTotal);
            ss.put("paidAmount", sPaid);
            ss.put("unpaidAmount", sTotal.subtract(sPaid));
            ss.put("orders", entry.getValue());
            supplierSummary.add(ss);

            grandTotal = grandTotal.add(sTotal);
            grandPaid = grandPaid.add(sPaid);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAmount", grandTotal);
        summary.put("paidAmount", grandPaid);
        summary.put("unpaidAmount", grandTotal.subtract(grandPaid));
        summary.put("orderCount", orders.size());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("suppliers", supplierSummary);
        result.put("truncated", truncated);
        return result;
    }

    @Override
    public Map<Integer, List<PurchaseOrderItem>> getItemsByOrderIds(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptyMap();
        LambdaQueryWrapper<PurchaseOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PurchaseOrderItem::getPurchaseOrderId, orderIds);
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(wrapper);
        return items.stream().collect(Collectors.groupingBy(PurchaseOrderItem::getPurchaseOrderId));
    }

    private void syncPurchaseOrderPaid(Integer orderId) {
        // 先锁订单行，防止并发支付导致 paidAmount 丢失更新
        LambdaQueryWrapper<PurchaseOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseOrder::getId, orderId).last("FOR UPDATE");
        PurchaseOrder order = purchaseOrderMapper.selectOne(lockWrapper);
        if (order == null) return;

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId).eq(Payment::getType, "payment");
        List<Payment> payments = paymentMapper.selectList(wrapper);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

    private String getConfigPrefix(String configKey, String defaultPrefix) {
        String val = sysConfigService.get(configKey);
        return (val != null && !val.isEmpty()) ? val : defaultPrefix;
    }

    private String generateOrderNo(String seqType, String prefix) {
        LambdaQueryWrapper<OrderSequence> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSequence::getSeqType, seqType).last("FOR UPDATE");
        OrderSequence seq = orderSequenceMapper.selectOne(wrapper);
        if (seq == null) {
            seq = new OrderSequence();
            seq.setSeqType(seqType);
            seq.setPrefix(prefix);
            seq.setCurrentSeq(1);
            seq.setLastDate(LocalDate.now());
            try {
                orderSequenceMapper.insert(seq);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                // 并发首次创建，另一线程已插入，重新读取
                seq = orderSequenceMapper.selectOne(wrapper);
            }
        } else if (!LocalDate.now().equals(seq.getLastDate())) {
            seq.setCurrentSeq(1);
        }
        int seqNum = seq.getCurrentSeq();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNo = prefix + dateStr + String.format("%03d", seqNum);
        seq.setCurrentSeq(seqNum + 1);
        seq.setLastDate(LocalDate.now());
        orderSequenceMapper.updateById(seq);
        return orderNo;
    }
}
