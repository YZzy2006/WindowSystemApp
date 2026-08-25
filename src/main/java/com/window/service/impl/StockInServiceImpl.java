package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.Commodity;
import com.window.entity.OrderSequence;
import com.window.entity.StockIn;
import com.window.entity.StockInItem;
import com.window.mapper.CommodityMapper;
import com.window.mapper.OrderSequenceMapper;
import com.window.mapper.StockInItemMapper;
import com.window.mapper.StockInMapper;
import com.window.service.StockInService;
import com.window.service.SysConfigService;
import com.window.exception.OrderNoExistsException;
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
public class StockInServiceImpl implements StockInService {
    private final StockInMapper stockInMapper;
    private final StockInItemMapper stockInItemMapper;
    private final CommodityMapper commodityMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final SysConfigService sysConfigService;

    @Override
    public IPage<StockIn> list(Page<StockIn> page, String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.like(StockIn::getOrderNo, escaped);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(StockIn::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(StockIn::getOrderDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(StockIn::getOrderDate).orderByDesc(StockIn::getCreateTime);
        return stockInMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getDetail(Integer id) {
        StockIn stockIn = stockInMapper.selectById(id);
        if (stockIn == null) return null;
        LambdaQueryWrapper<StockInItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInItem::getStockInId, id);
        List<StockInItem> items = stockInItemMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("order", stockIn);
        result.put("items", items);
        return result;
    }

    @Override
    public StockIn getById(Integer id) {
        return stockInMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(StockIn stockIn, List<StockInItem> items) {
        if (!StringUtils.hasText(stockIn.getOrderNo())) {
            String prefix = getConfigPrefix("stock_in_prefix", "RK");
            stockIn.setOrderNo(generateOrderNo("stock_in", prefix));
        }
        // 单号唯一预检（含软删除行，唯一索引仍占用）
        if (StringUtils.hasText(stockIn.getOrderNo()) && stockInMapper.countByOrderNo(stockIn.getOrderNo()) > 0) {
            throw new OrderNoExistsException("单号已存在：" + stockIn.getOrderNo());
        }
        stockInMapper.insert(stockIn);
        if (items != null) {
            // 按 commodityId 排序后加锁，防止并发死锁
            lockCommoditiesInOrder(items.stream().map(StockInItem::getCommodityId).collect(Collectors.toList()));
            for (StockInItem item : items) {
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                item.setStockInId(stockIn.getId());
                stockInItemMapper.insert(item);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(StockIn stockIn, List<StockInItem> items) {
        // 锁定入库单行，防止并发编辑
        LambdaQueryWrapper<StockIn> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(StockIn::getId, stockIn.getId()).last("FOR UPDATE");
        StockIn existing = stockInMapper.selectOne(lockWrapper);
        if (existing == null) throw new IllegalArgumentException("入库单不存在");

        // 收集所有需要加锁的 commodityId（旧+新），排序后统一加锁
        LambdaQueryWrapper<StockInItem> oldWrapper = new LambdaQueryWrapper<>();
        oldWrapper.eq(StockInItem::getStockInId, stockIn.getId());
        List<StockInItem> oldItems = stockInItemMapper.selectList(oldWrapper);
        List<Integer> allCommodityIds = new ArrayList<>();
        for (StockInItem oldItem : oldItems) {
            if (oldItem.getCommodityId() != null) allCommodityIds.add(oldItem.getCommodityId());
        }
        if (items != null) {
            for (StockInItem item : items) {
                if (item.getCommodityId() != null) allCommodityIds.add(item.getCommodityId());
            }
        }
        lockCommoditiesInOrder(allCommodityIds);

        // 先回退旧明细的库存
        for (StockInItem oldItem : oldItems) {
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
        stockInItemMapper.delete(oldWrapper);
        // 更新主表
        stockInMapper.updateById(stockIn);
        // 插入新明细并更新库存
        if (items != null) {
            for (StockInItem item : items) {
                item.setId(null);
                if (item.getQuantity() != null && item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()));
                }
                item.setStockInId(stockIn.getId());
                stockInItemMapper.insert(item);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        // 加锁防止并发编辑/删除导致库存错乱
        LambdaQueryWrapper<StockIn> lw = new LambdaQueryWrapper<>();
        lw.eq(StockIn::getId, id).last("FOR UPDATE");
        StockIn order = stockInMapper.selectOne(lw);
        if (order == null) throw new IllegalArgumentException("入库单不存在");
        // 回退库存
        LambdaQueryWrapper<StockInItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StockInItem::getStockInId, id);
        List<StockInItem> items = stockInItemMapper.selectList(wrapper);
        // 按 commodityId 排序后加锁，防止并发死锁
        lockCommoditiesInOrder(items.stream().map(StockInItem::getCommodityId).collect(Collectors.toList()));
        for (StockInItem item : items) {
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
        stockInItemMapper.delete(wrapper);
        stockInMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> getSummary(String startDate, String endDate) {
        LambdaQueryWrapper<StockIn> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(StockIn::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(StockIn::getOrderDate, LocalDate.parse(endDate));
        }
        List<StockIn> orders = stockInMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = orders.size();

        // 批量查询所有明细，避免 N+1
        List<Integer> orderIds = orders.stream().map(StockIn::getId).collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<StockInItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(StockInItem::getStockInId, orderIds);
            for (StockInItem item : stockInItemMapper.selectList(itemWrapper)) {
                totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("totalAmount", totalAmount);
        return summary;
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
