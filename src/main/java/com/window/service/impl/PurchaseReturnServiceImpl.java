package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.common.StatusValidator;
import com.window.exception.OrderNoExistsException;
import com.window.entity.Commodity;
import com.window.entity.OrderSequence;
import com.window.entity.PurchaseReturn;
import com.window.entity.PurchaseReturnItem;
import com.window.mapper.CommodityMapper;
import com.window.mapper.OrderSequenceMapper;
import com.window.mapper.PurchaseReturnItemMapper;
import com.window.mapper.PurchaseReturnMapper;
import com.window.entity.Payment;
import com.window.mapper.PaymentMapper;
import com.window.service.PurchaseReturnService;
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
public class PurchaseReturnServiceImpl implements PurchaseReturnService {
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final PurchaseReturnItemMapper purchaseReturnItemMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final CommodityMapper commodityMapper;
    private final SysConfigService sysConfigService;
    private final PaymentMapper paymentMapper;

    @Override
    public IPage<PurchaseReturn> list(Page<PurchaseReturn> page, String keyword, String status, String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(PurchaseReturn::getOrderNo, escaped)
                    .or().like(PurchaseReturn::getSupplierName, escaped)
                    .or().like(PurchaseReturn::getOriginalOrderNo, escaped));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PurchaseReturn::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseReturn::getReturnDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseReturn::getReturnDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(PurchaseReturn::getReturnDate).orderByDesc(PurchaseReturn::getCreateTime);
        return purchaseReturnMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getDetail(Integer id) {
        PurchaseReturn purchaseReturn = purchaseReturnMapper.selectById(id);
        if (purchaseReturn == null) return null;
        LambdaQueryWrapper<PurchaseReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturnItem::getReturnId, id);
        List<PurchaseReturnItem> items = purchaseReturnItemMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("order", purchaseReturn);
        result.put("items", items);
        return result;
    }

    @Override
    public PurchaseReturn getById(Integer id) {
        return purchaseReturnMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PurchaseReturn purchaseReturn, List<PurchaseReturnItem> items) {
        if (!StringUtils.hasText(purchaseReturn.getStatus())) {
            purchaseReturn.setStatus("pending");
        }
        if (!StringUtils.hasText(purchaseReturn.getOrderNo())) {
            String prefix = getConfigPrefix("purchase_return_prefix", "CT");
            purchaseReturn.setOrderNo(generateOrderNo("purchase_return", prefix));
        }
        // 订单号唯一预检（含软删除行，唯一索引仍占用），导入重复订单号时给出明确提示
        if (StringUtils.hasText(purchaseReturn.getOrderNo()) && purchaseReturnMapper.countByOrderNo(purchaseReturn.getOrderNo()) > 0) {
            throw new OrderNoExistsException("订单号已存在：" + purchaseReturn.getOrderNo());
        }
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseReturnItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        purchaseReturn.setTotalAmount(total);
        purchaseReturnMapper.insert(purchaseReturn);
        if (items != null) {
            // 按 commodityId 排序后加锁，防止并发死锁
            lockCommoditiesInOrder(items.stream().map(PurchaseReturnItem::getCommodityId).collect(Collectors.toList()));
            for (PurchaseReturnItem item : items) {
                item.setReturnId(purchaseReturn.getId());
                purchaseReturnItemMapper.insert(item);
                // 采购退货 → 商品出库，扣减库存
                if (item.getCommodityId() != null && item.getQuantity() != null) {
                    Commodity c = commodityMapper.selectByIdForUpdate(item.getCommodityId());
                    if (c != null) {
                        BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                        if (currentQty.compareTo(item.getQuantity()) < 0) {
                            throw new IllegalArgumentException(
                                    "商品【" + c.getName() + "】库存不足，无法完成退货");
                        }
                        Commodity update = new Commodity();
                        update.setId(item.getCommodityId());
                        update.setCurrentQty(currentQty.subtract(item.getQuantity()));
                        commodityMapper.updateById(update);
                    }
                }
            }
        }
        syncPurchaseReturnPaid(purchaseReturn.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(PurchaseReturn purchaseReturn, List<PurchaseReturnItem> items) {
        // 锁定退货单行，防止并发编辑同一退货单
        LambdaQueryWrapper<PurchaseReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseReturn::getId, purchaseReturn.getId()).last("FOR UPDATE");
        PurchaseReturn existing = purchaseReturnMapper.selectOne(lockWrapper);
        if (existing == null) throw new IllegalArgumentException("退货单不存在");
        if ("cancelled".equals(existing.getStatus()) || "completed".equals(existing.getStatus())) {
            throw new IllegalStateException("已取消或已完成的退货单不允许编辑");
        }

        // 先回退旧明细的库存
        LambdaQueryWrapper<PurchaseReturnItem> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(PurchaseReturnItem::getReturnId, purchaseReturn.getId());
        List<PurchaseReturnItem> oldItems = purchaseReturnItemMapper.selectList(oldWrapper);

        // 收集所有需要加锁的 commodityId（旧+新），排序后统一加锁，防止死锁
        List<Integer> allCommodityIds = new ArrayList<>();
        for (PurchaseReturnItem oldItem : oldItems) {
            if (oldItem.getCommodityId() != null) allCommodityIds.add(oldItem.getCommodityId());
        }
        if (items != null) {
            for (PurchaseReturnItem item : items) {
                if (item.getCommodityId() != null) allCommodityIds.add(item.getCommodityId());
            }
        }
        lockCommoditiesInOrder(allCommodityIds);

        // 回退旧库存
        for (PurchaseReturnItem oldItem : oldItems) {
            if (oldItem.getCommodityId() != null && oldItem.getQuantity() != null) {
                Commodity c = commodityMapper.selectByIdForUpdate(oldItem.getCommodityId());
                if (c != null) {
                    BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                    Commodity update = new Commodity();
                    update.setId(oldItem.getCommodityId());
                    update.setCurrentQty(currentQty.add(oldItem.getQuantity()));
                    commodityMapper.updateById(update);
                }
            }
        }
        // 删除旧明细
        purchaseReturnItemMapper.delete(oldWrapper);
        // 计算新总额
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (PurchaseReturnItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        purchaseReturn.setTotalAmount(total);
        if (!StringUtils.hasText(purchaseReturn.getStatus())) {
            purchaseReturn.setStatus(existing.getStatus());
        } else if (!purchaseReturn.getStatus().equals(existing.getStatus())) {
            StatusValidator.validateReturn(existing.getStatus(), purchaseReturn.getStatus());
        }
        purchaseReturnMapper.updateById(purchaseReturn);
        // 插入新明细并更新库存
        if (items != null) {
            for (PurchaseReturnItem item : items) {
                item.setId(null);
                item.setReturnId(purchaseReturn.getId());
                purchaseReturnItemMapper.insert(item);
                if (item.getCommodityId() != null && item.getQuantity() != null) {
                    Commodity c = commodityMapper.selectByIdForUpdate(item.getCommodityId());
                    if (c != null) {
                        BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                        if (currentQty.compareTo(item.getQuantity()) < 0) {
                            throw new IllegalArgumentException(
                                    "商品【" + c.getName() + "】库存不足，无法完成退货");
                        }
                        Commodity update = new Commodity();
                        update.setId(item.getCommodityId());
                        update.setCurrentQty(currentQty.subtract(item.getQuantity()));
                        commodityMapper.updateById(update);
                    }
                }
            }
        }
        syncPurchaseReturnPaid(purchaseReturn.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        // 加锁防止并发编辑/删除导致库存错乱
        LambdaQueryWrapper<PurchaseReturn> lw = new LambdaQueryWrapper<>();
        lw.eq(PurchaseReturn::getId, id).last("FOR UPDATE");
        PurchaseReturn order = purchaseReturnMapper.selectOne(lw);
        if (order == null) throw new IllegalArgumentException("退货单不存在");
        if (!"cancelled".equals(order.getStatus())) {
            throw new IllegalStateException("只有已取消的退货单才能删除");
        }
        // 删除关联的收付款记录
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getOrderId, id);
        paymentMapper.delete(paymentWrapper);
        // 库存已在 updateStatus("cancelled") 中回退，这里只删明细
        LambdaQueryWrapper<PurchaseReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseReturnItem::getReturnId, id);
        purchaseReturnItemMapper.delete(wrapper);
        purchaseReturnMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, String status) {
        LambdaQueryWrapper<PurchaseReturn> lw = new LambdaQueryWrapper<>();
        lw.eq(PurchaseReturn::getId, id).last("FOR UPDATE");
        PurchaseReturn existing = purchaseReturnMapper.selectOne(lw);
        if (existing == null) throw new IllegalArgumentException("退货单不存在");
        StatusValidator.validateReturn(existing.getStatus(), status);
        purchaseReturnMapper.updateStatus(id, status);

        // 取消时回退库存：save() 时扣了库存，这里加回去
        if ("cancelled".equals(status)) {
            LambdaQueryWrapper<PurchaseReturnItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(PurchaseReturnItem::getReturnId, id);
            List<PurchaseReturnItem> items = purchaseReturnItemMapper.selectList(itemWrapper);
            if (items != null) {
                lockCommoditiesInOrder(items.stream().map(PurchaseReturnItem::getCommodityId).collect(Collectors.toList()));
                for (PurchaseReturnItem item : items) {
                    if (item.getCommodityId() != null && item.getQuantity() != null) {
                        Commodity c = commodityMapper.selectByIdForUpdate(item.getCommodityId());
                        if (c != null) {
                            BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                            Commodity update = new Commodity();
                            update.setId(item.getCommodityId());
                            update.setCurrentQty(currentQty.add(item.getQuantity()));
                            commodityMapper.updateById(update);
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCleared(Integer id) {
        LambdaQueryWrapper<PurchaseReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseReturn::getId, id).last("FOR UPDATE");
        PurchaseReturn order = purchaseReturnMapper.selectOne(lockWrapper);
        if (order == null) throw new IllegalArgumentException("退货单不存在");
        if ("cancelled".equals(order.getStatus())) {
            throw new IllegalStateException("已取消的退货单不能结算");
        }
        int newVal = order.getIsCleared() != null && order.getIsCleared() == 1 ? 0 : 1;
        purchaseReturnMapper.updateCleared(id, newVal);
        if (newVal == 1 && !"completed".equals(order.getStatus())) {
            purchaseReturnMapper.updateStatus(id, "completed");
        }
    }

    @Override
    public Map<String, Object> getSummary(String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(PurchaseReturn::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(PurchaseReturn::getReturnDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(PurchaseReturn::getReturnDate, LocalDate.parse(endDate));
        }
        List<PurchaseReturn> returns = purchaseReturnMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        int totalCount = returns.size();

        // 批量查询所有付款记录，避免 N+1
        List<Integer> returnIds = returns.stream().map(PurchaseReturn::getId).collect(Collectors.toList());
        Map<Integer, BigDecimal> paidById = new HashMap<>();
        if (!returnIds.isEmpty()) {
            LambdaQueryWrapper<Payment> pw = new LambdaQueryWrapper<>();
            pw.in(Payment::getOrderId, returnIds).eq(Payment::getType, "receipt");
            paymentMapper.selectList(pw).stream()
                    .filter(p -> p.getAmount() != null)
                    .forEach(p -> paidById.merge(p.getOrderId(), p.getAmount(), BigDecimal::add));
        }

        for (PurchaseReturn r : returns) {
            totalAmount = totalAmount.add(r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO);
            totalPaid = totalPaid.add(paidById.getOrDefault(r.getId(), BigDecimal.ZERO));
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("totalAmount", totalAmount);
        summary.put("totalPaid", totalPaid);
        summary.put("totalUnpaid", totalAmount.subtract(totalPaid));
        return summary;
    }

    @Override
    public Map<String, Object> getAnalysis(String startDate, String endDate) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(PurchaseReturn::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) wrapper.ge(PurchaseReturn::getReturnDate, LocalDate.parse(startDate));
        if (StringUtils.hasText(endDate)) wrapper.le(PurchaseReturn::getReturnDate, LocalDate.parse(endDate));
        wrapper.orderByDesc(PurchaseReturn::getReturnDate);
        List<PurchaseReturn> returns = purchaseReturnMapper.selectList(wrapper);

        List<Integer> returnIds = returns.stream().map(PurchaseReturn::getId).collect(Collectors.toList());
        Map<Integer, List<PurchaseReturnItem>> itemsByReturn = new HashMap<>();
        if (!returnIds.isEmpty()) {
            LambdaQueryWrapper<PurchaseReturnItem> iw = new LambdaQueryWrapper<>();
            iw.in(PurchaseReturnItem::getReturnId, returnIds);
            itemsByReturn = purchaseReturnItemMapper.selectList(iw).stream()
                    .collect(Collectors.groupingBy(PurchaseReturnItem::getReturnId));
        }

        Map<String, Map<String, Object>> productMap = new LinkedHashMap<>();
        Map<String, Map<String, Object>> supplierMap = new LinkedHashMap<>();

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQtyCount = 0;

        for (PurchaseReturn r : returns) {
            BigDecimal rAmt = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(rAmt);

            String sname = r.getSupplierName() != null ? r.getSupplierName() : "未知";
            Map<String, Object> sp = supplierMap.computeIfAbsent(sname, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("partyName", k);
                m.put("returnCount", 0);
                m.put("totalAmount", BigDecimal.ZERO);
                return m;
            });
            sp.put("returnCount", (int) sp.get("returnCount") + 1);
            sp.put("totalAmount", ((BigDecimal) sp.get("totalAmount")).add(rAmt));

            for (PurchaseReturnItem item : itemsByReturn.getOrDefault(r.getId(), Collections.emptyList())) {
                BigDecimal iAmt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
                BigDecimal iQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                totalQtyCount += iQty.intValue();

                String key = item.getCommodityId() != null ? String.valueOf(item.getCommodityId()) : ("_" + item.getProductName());
                Map<String, Object> pp = productMap.computeIfAbsent(key, k -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("commodityId", item.getCommodityId());
                    m.put("productName", item.getProductName());
                    m.put("productCategory", item.getProductCategory());
                    m.put("returnCount", 0);
                    m.put("totalQty", BigDecimal.ZERO);
                    m.put("totalAmount", BigDecimal.ZERO);
                    return m;
                });
                pp.put("returnCount", (int) pp.get("returnCount") + 1);
                pp.put("totalQty", ((BigDecimal) pp.get("totalQty")).add(iQty));
                pp.put("totalAmount", ((BigDecimal) pp.get("totalAmount")).add(iAmt));
            }
        }

        List<Map<String, Object>> productRank = productMap.values().stream()
                .sorted((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")))
                .collect(Collectors.toList());
        List<Map<String, Object>> supplierRank = supplierMap.values().stream()
                .sorted((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", returns.size());
        summary.put("totalAmount", totalAmount);
        summary.put("productKinds", productMap.size());
        summary.put("totalQty", totalQtyCount);

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("productRank", productRank);
        result.put("partyRank", supplierRank);
        return result;
    }

    @Override
    public Map<Integer, List<PurchaseReturnItem>> getItemsByOrderIds(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptyMap();
        LambdaQueryWrapper<PurchaseReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PurchaseReturnItem::getReturnId, orderIds);
        List<PurchaseReturnItem> items = purchaseReturnItemMapper.selectList(wrapper);
        return items.stream().collect(Collectors.groupingBy(PurchaseReturnItem::getReturnId));
    }

    /**
     * 按 commodityId 升序加锁，保证全局锁顺序一致，防止并发死锁
     */
    private void lockCommoditiesInOrder(List<Integer> commodityIds) {
        if (commodityIds == null || commodityIds.isEmpty()) return;
        commodityIds.stream().filter(Objects::nonNull).distinct().sorted().forEach(id -> {
            commodityMapper.selectByIdForUpdate(id);
        });
    }

    private void syncPurchaseReturnPaid(Integer orderId) {
        // 先锁订单行，防止并发支付导致 paidAmount 丢失更新
        LambdaQueryWrapper<PurchaseReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(PurchaseReturn::getId, orderId).last("FOR UPDATE");
        PurchaseReturn order = purchaseReturnMapper.selectOne(lockWrapper);
        if (order == null) return;

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId).eq(Payment::getType, "receipt");
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
        purchaseReturnMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(order.getStatus()) && !"cancelled".equals(order.getStatus())) {
            purchaseReturnMapper.updateStatus(orderId, "completed");
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
