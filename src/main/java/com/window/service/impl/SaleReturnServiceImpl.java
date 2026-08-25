package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.common.StatusValidator;
import com.window.exception.OrderNoExistsException;
import com.window.entity.Commodity;
import com.window.entity.OrderSequence;
import com.window.entity.Payment;
import com.window.entity.SaleReturn;
import com.window.entity.SaleReturnItem;
import com.window.mapper.CommodityMapper;
import com.window.mapper.OrderSequenceMapper;
import com.window.mapper.PaymentMapper;
import com.window.mapper.SaleReturnItemMapper;
import com.window.mapper.SaleReturnMapper;
import com.window.service.SaleReturnService;
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
public class SaleReturnServiceImpl implements SaleReturnService {
    private final SaleReturnMapper saleReturnMapper;
    private final SaleReturnItemMapper saleReturnItemMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final CommodityMapper commodityMapper;
    private final SysConfigService sysConfigService;
    private final PaymentMapper paymentMapper;

    @Override
    public IPage<SaleReturn> list(Page<SaleReturn> page, String keyword, String status, String startDate, String endDate) {
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(SaleReturn::getOrderNo, escaped)
                    .or().like(SaleReturn::getCustomerName, escaped)
                    .or().like(SaleReturn::getOriginalOrderNo, escaped));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SaleReturn::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SaleReturn::getReturnDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SaleReturn::getReturnDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(SaleReturn::getReturnDate).orderByDesc(SaleReturn::getCreateTime);
        return saleReturnMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getDetail(Integer id) {
        SaleReturn saleReturn = saleReturnMapper.selectById(id);
        if (saleReturn == null) return null;
        LambdaQueryWrapper<SaleReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleReturnItem::getReturnId, id);
        List<SaleReturnItem> items = saleReturnItemMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("order", saleReturn);
        result.put("items", items);
        return result;
    }

    @Override
    public SaleReturn getById(Integer id) {
        return saleReturnMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SaleReturn saleReturn, List<SaleReturnItem> items) {
        if (!StringUtils.hasText(saleReturn.getStatus())) {
            saleReturn.setStatus("pending");
        }
        if (!StringUtils.hasText(saleReturn.getOrderNo())) {
            String prefix = getConfigPrefix("sale_return_prefix", "XT");
            saleReturn.setOrderNo(generateOrderNo("sale_return", prefix));
        }
        // 订单号唯一预检（含软删除行，唯一索引仍占用），导入重复订单号时给出明确提示
        if (StringUtils.hasText(saleReturn.getOrderNo()) && saleReturnMapper.countByOrderNo(saleReturn.getOrderNo()) > 0) {
            throw new OrderNoExistsException("订单号已存在：" + saleReturn.getOrderNo());
        }
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (SaleReturnItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        saleReturn.setTotalAmount(total);
        saleReturnMapper.insert(saleReturn);
        if (items != null) {
            // 按 commodityId 排序后加锁，防止并发死锁
            lockCommoditiesInOrder(items.stream().map(SaleReturnItem::getCommodityId).collect(Collectors.toList()));
            for (SaleReturnItem item : items) {
                item.setReturnId(saleReturn.getId());
                saleReturnItemMapper.insert(item);
                // 销售退货 → 商品回库，增加库存
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
        syncPaidAmount(saleReturn.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(SaleReturn saleReturn, List<SaleReturnItem> items) {
        // 锁定退货单行，防止并发编辑同一退货单
        LambdaQueryWrapper<SaleReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleReturn::getId, saleReturn.getId()).last("FOR UPDATE");
        SaleReturn existing = saleReturnMapper.selectOne(lockWrapper);
        if (existing == null) throw new IllegalArgumentException("退货单不存在");
        if ("cancelled".equals(existing.getStatus()) || "completed".equals(existing.getStatus())) {
            throw new IllegalStateException("已取消或已完成的退货单不允许编辑");
        }

        // 先回退旧明细的库存
        LambdaQueryWrapper<SaleReturnItem> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(SaleReturnItem::getReturnId, saleReturn.getId());
        List<SaleReturnItem> oldItems = saleReturnItemMapper.selectList(oldWrapper);

        // 收集所有需要加锁的 commodityId（旧+新），排序后统一加锁，防止死锁
        List<Integer> allCommodityIds = new ArrayList<>();
        for (SaleReturnItem oldItem : oldItems) {
            if (oldItem.getCommodityId() != null) allCommodityIds.add(oldItem.getCommodityId());
        }
        if (items != null) {
            for (SaleReturnItem item : items) {
                if (item.getCommodityId() != null) allCommodityIds.add(item.getCommodityId());
            }
        }
        lockCommoditiesInOrder(allCommodityIds);

        // 回退旧库存
        for (SaleReturnItem oldItem : oldItems) {
            if (oldItem.getCommodityId() != null && oldItem.getQuantity() != null) {
                Commodity c = commodityMapper.selectByIdForUpdate(oldItem.getCommodityId());
                if (c != null) {
                    BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                    Commodity update = new Commodity();
                    update.setId(oldItem.getCommodityId());
                    update.setCurrentQty(currentQty.subtract(oldItem.getQuantity()));
                    commodityMapper.updateById(update);
                }
            }
        }
        // 删除旧明细
        saleReturnItemMapper.delete(oldWrapper);
        // 计算新总额
        BigDecimal total = BigDecimal.ZERO;
        if (items != null) {
            for (SaleReturnItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                if (item.getAmount() != null) {
                    total = total.add(item.getAmount());
                }
            }
        }
        saleReturn.setTotalAmount(total);
        if (!StringUtils.hasText(saleReturn.getStatus())) {
            saleReturn.setStatus(existing.getStatus());
        } else if (!saleReturn.getStatus().equals(existing.getStatus())) {
            StatusValidator.validateReturn(existing.getStatus(), saleReturn.getStatus());
        }
        saleReturnMapper.updateById(saleReturn);
        // 插入新明细并更新库存
        if (items != null) {
            for (SaleReturnItem item : items) {
                item.setId(null);
                item.setReturnId(saleReturn.getId());
                saleReturnItemMapper.insert(item);
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
        // 总额变更后重新同步已付款金额
        syncPaidAmount(saleReturn.getId());
    }

    private void syncPaidAmount(Integer returnId) {
        // 先锁退货单行，再查付款记录（与 PaymentServiceImpl 保持一致）
        LambdaQueryWrapper<SaleReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleReturn::getId, returnId).last("FOR UPDATE");
        SaleReturn sr = saleReturnMapper.selectOne(lockWrapper);
        if (sr == null) return;

        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getOrderId, returnId).eq(Payment::getType, "payment");
        List<Payment> payments = paymentMapper.selectList(paymentWrapper);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean cleared = false;
        if (sr.getTotalAmount() != null && sr.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = sr.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        saleReturnMapper.updatePaidAmount(returnId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(sr.getStatus()) && !"cancelled".equals(sr.getStatus())) {
            saleReturnMapper.updateStatus(returnId, "completed");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        // 加锁防止并发编辑/删除导致库存错乱
        LambdaQueryWrapper<SaleReturn> lw = new LambdaQueryWrapper<>();
        lw.eq(SaleReturn::getId, id).last("FOR UPDATE");
        SaleReturn order = saleReturnMapper.selectOne(lw);
        if (order == null) throw new IllegalArgumentException("退货单不存在");
        if (!"cancelled".equals(order.getStatus())) {
            throw new IllegalStateException("只有已取消的退货单才能删除");
        }
        // 删除关联的收付款记录
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getOrderId, id);
        paymentMapper.delete(paymentWrapper);
        // 库存已在 updateStatus("cancelled") 中回退，这里只删明细
        LambdaQueryWrapper<SaleReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleReturnItem::getReturnId, id);
        saleReturnItemMapper.delete(wrapper);
        saleReturnMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, String status) {
        LambdaQueryWrapper<SaleReturn> lw = new LambdaQueryWrapper<>();
        lw.eq(SaleReturn::getId, id).last("FOR UPDATE");
        SaleReturn existing = saleReturnMapper.selectOne(lw);
        if (existing == null) throw new IllegalArgumentException("退货单不存在");
        StatusValidator.validateReturn(existing.getStatus(), status);
        saleReturnMapper.updateStatus(id, status);

        // 取消时回退库存：save() 时加了库存，这里扣回去
        if ("cancelled".equals(status)) {
            LambdaQueryWrapper<SaleReturnItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(SaleReturnItem::getReturnId, id);
            List<SaleReturnItem> items = saleReturnItemMapper.selectList(itemWrapper);
            if (items != null) {
                lockCommoditiesInOrder(items.stream().map(SaleReturnItem::getCommodityId).collect(Collectors.toList()));
                for (SaleReturnItem item : items) {
                    if (item.getCommodityId() != null && item.getQuantity() != null) {
                        Commodity c = commodityMapper.selectByIdForUpdate(item.getCommodityId());
                        if (c != null) {
                            BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
                            Commodity update = new Commodity();
                            update.setId(item.getCommodityId());
                            update.setCurrentQty(currentQty.subtract(item.getQuantity()));
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
        LambdaQueryWrapper<SaleReturn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleReturn::getId, id).last("FOR UPDATE");
        SaleReturn order = saleReturnMapper.selectOne(lockWrapper);
        if (order == null) throw new IllegalArgumentException("退货单不存在");
        if ("cancelled".equals(order.getStatus())) {
            throw new IllegalStateException("已取消的退货单不能结算");
        }
        int newVal = order.getIsCleared() != null && order.getIsCleared() == 1 ? 0 : 1;
        saleReturnMapper.updateCleared(id, newVal);
        if (newVal == 1 && !"completed".equals(order.getStatus())) {
            saleReturnMapper.updateStatus(id, "completed");
        }
    }

    @Override
    public Map<String, Object> getSummary(String startDate, String endDate) {
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SaleReturn::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SaleReturn::getReturnDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SaleReturn::getReturnDate, LocalDate.parse(endDate));
        }
        List<SaleReturn> returns = saleReturnMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        int totalCount = returns.size();

        // 批量查询所有付款记录，避免 N+1
        List<Integer> returnIds = returns.stream().map(SaleReturn::getId).collect(Collectors.toList());
        Map<Integer, BigDecimal> paidById = new HashMap<>();
        if (!returnIds.isEmpty()) {
            LambdaQueryWrapper<Payment> pw = new LambdaQueryWrapper<>();
            pw.in(Payment::getOrderId, returnIds).eq(Payment::getType, "payment");
            paymentMapper.selectList(pw).stream()
                    .filter(p -> p.getAmount() != null)
                    .forEach(p -> paidById.merge(p.getOrderId(), p.getAmount(), BigDecimal::add));
        }

        for (SaleReturn r : returns) {
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
        LambdaQueryWrapper<SaleReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SaleReturn::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) wrapper.ge(SaleReturn::getReturnDate, LocalDate.parse(startDate));
        if (StringUtils.hasText(endDate)) wrapper.le(SaleReturn::getReturnDate, LocalDate.parse(endDate));
        wrapper.orderByDesc(SaleReturn::getReturnDate);
        List<SaleReturn> returns = saleReturnMapper.selectList(wrapper);

        List<Integer> returnIds = returns.stream().map(SaleReturn::getId).collect(Collectors.toList());
        Map<Integer, List<SaleReturnItem>> itemsByReturn = new HashMap<>();
        if (!returnIds.isEmpty()) {
            LambdaQueryWrapper<SaleReturnItem> iw = new LambdaQueryWrapper<>();
            iw.in(SaleReturnItem::getReturnId, returnIds);
            itemsByReturn = saleReturnItemMapper.selectList(iw).stream()
                    .collect(Collectors.groupingBy(SaleReturnItem::getReturnId));
        }

        // 按商品分组
        Map<String, Map<String, Object>> productMap = new LinkedHashMap<>();
        // 按客户分组
        Map<String, Map<String, Object>> customerMap = new LinkedHashMap<>();

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQtyCount = 0;

        for (SaleReturn r : returns) {
            BigDecimal rAmt = r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO;
            totalAmount = totalAmount.add(rAmt);

            // 客户分组
            String cname = r.getCustomerName() != null ? r.getCustomerName() : "未知";
            Map<String, Object> cp = customerMap.computeIfAbsent(cname, k -> {
                Map<String, Object> m = new HashMap<>();
                m.put("partyName", k);
                m.put("returnCount", 0);
                m.put("totalAmount", BigDecimal.ZERO);
                return m;
            });
            cp.put("returnCount", (int) cp.get("returnCount") + 1);
            cp.put("totalAmount", ((BigDecimal) cp.get("totalAmount")).add(rAmt));

            // 商品分组
            for (SaleReturnItem item : itemsByReturn.getOrDefault(r.getId(), Collections.emptyList())) {
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

        // 按金额排序
        List<Map<String, Object>> productRank = productMap.values().stream()
                .sorted((a, b) -> ((BigDecimal) b.get("totalAmount")).compareTo((BigDecimal) a.get("totalAmount")))
                .collect(Collectors.toList());
        List<Map<String, Object>> customerRank = customerMap.values().stream()
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
        result.put("partyRank", customerRank);
        return result;
    }

    @Override
    public Map<Integer, List<SaleReturnItem>> getItemsByOrderIds(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptyMap();
        LambdaQueryWrapper<SaleReturnItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SaleReturnItem::getReturnId, orderIds);
        List<SaleReturnItem> items = saleReturnItemMapper.selectList(wrapper);
        return items.stream().collect(Collectors.groupingBy(SaleReturnItem::getReturnId));
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
