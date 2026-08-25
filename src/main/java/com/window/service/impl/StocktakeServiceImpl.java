package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.*;
import com.window.mapper.*;
import com.window.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StocktakeServiceImpl implements StocktakeService {
    private final CommodityMapper commodityMapper;
    private final CommodityCategoryMapper categoryMapper;
    private final StockInMapper stockInMapper;
    private final StockInItemMapper stockInItemMapper;
    private final StockOutMapper stockOutMapper;
    private final StockOutItemMapper stockOutItemMapper;

    @Override
    public List<Map<String, Object>> getFIFODetail(Integer commodityId) {
        // 获取商品所有入库批次，按日期排序（FIFO）
        LambdaQueryWrapper<StockInItem> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(StockInItem::getCommodityId, commodityId);
        List<StockInItem> allInItems = stockInItemMapper.selectList(inWrapper);

        // 获取入库单日期
        Map<Integer, LocalDate> stockInDates = new HashMap<>();
        Set<Integer> stockInIds = allInItems.stream()
                .map(StockInItem::getStockInId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Integer stockInId : stockInIds) {
            StockIn stockIn = stockInMapper.selectById(stockInId);
            if (stockIn != null && stockIn.getOrderDate() != null) {
                stockInDates.put(stockInId, stockIn.getOrderDate());
            }
        }

        // 按日期排序入库批次
        allInItems.sort((a, b) -> {
            LocalDate dateA = stockInDates.getOrDefault(a.getStockInId(), LocalDate.MIN);
            LocalDate dateB = stockInDates.getOrDefault(b.getStockInId(), LocalDate.MIN);
            return dateA.compareTo(dateB);
        });

        // 获取商品所有出库记录
        LambdaQueryWrapper<StockOutItem> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(StockOutItem::getCommodityId, commodityId);
        List<StockOutItem> allOutItems = stockOutItemMapper.selectList(outWrapper);

        // 获取出库单日期
        Map<Integer, LocalDate> stockOutDates = new HashMap<>();
        Set<Integer> stockOutIds = allOutItems.stream()
                .map(StockOutItem::getStockOutId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (Integer stockOutId : stockOutIds) {
            StockOut stockOut = stockOutMapper.selectById(stockOutId);
            if (stockOut != null && stockOut.getOrderDate() != null) {
                stockOutDates.put(stockOutId, stockOut.getOrderDate());
            }
        }

        // 按日期排序出库记录
        allOutItems.sort((a, b) -> {
            LocalDate dateA = stockOutDates.getOrDefault(a.getStockOutId(), LocalDate.MIN);
            LocalDate dateB = stockOutDates.getOrDefault(b.getStockOutId(), LocalDate.MIN);
            return dateA.compareTo(dateB);
        });

        // FIFO匹配：构建入库批次队列
        List<Map<String, Object>> fifoBatches = new ArrayList<>();
        BigDecimal totalInQty = BigDecimal.ZERO;
        BigDecimal totalInCost = BigDecimal.ZERO;
        BigDecimal totalOutQty = BigDecimal.ZERO;
        BigDecimal totalOutCost = BigDecimal.ZERO;

        // 入库批次队列
        Queue<StockInItem> inQueue = new LinkedList<>(allInItems);
        // 每个批次的剩余数量
        Map<Integer, BigDecimal> remainingQty = new HashMap<>();
        for (StockInItem item : allInItems) {
            remainingQty.put(item.getId(), item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO);
        }

        // 按时间顺序处理所有出入库
        List<Map<String, Object>> movements = new ArrayList<>();

        // 添加入库记录
        for (StockInItem item : allInItems) {
            Map<String, Object> movement = new HashMap<>();
            movement.put("type", "in");
            movement.put("date", stockInDates.get(item.getStockInId()));
            movement.put("quantity", item.getQuantity());
            movement.put("unitPrice", item.getUnitPrice());
            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            movement.put("amount", qty.multiply(price));
            movement.put("batchId", item.getId());
            movements.add(movement);

            totalInQty = totalInQty.add(qty);
            totalInCost = totalInCost.add(qty.multiply(price));
        }

        // 添加出库记录（需要FIFO匹配成本）
        for (StockOutItem item : allOutItems) {
            BigDecimal outQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal fifoCost = BigDecimal.ZERO;
            BigDecimal remaining = outQty;

            // FIFO匹配入库批次
            for (StockInItem inItem : allInItems) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
                BigDecimal batchRemaining = remainingQty.getOrDefault(inItem.getId(), BigDecimal.ZERO);
                if (batchRemaining.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal matchQty = remaining.min(batchRemaining);
                BigDecimal inPrice = inItem.getUnitPrice() != null ? inItem.getUnitPrice() : BigDecimal.ZERO;
                fifoCost = fifoCost.add(matchQty.multiply(inPrice));
                remainingQty.put(inItem.getId(), batchRemaining.subtract(matchQty));
                remaining = remaining.subtract(matchQty);
            }

            Map<String, Object> movement = new HashMap<>();
            movement.put("type", "out");
            movement.put("date", stockOutDates.get(item.getStockOutId()));
            movement.put("quantity", outQty);
            movement.put("fifoCost", fifoCost);
            movement.put("avgCost", outQty.compareTo(BigDecimal.ZERO) > 0 ?
                    fifoCost.divide(outQty, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            movements.add(movement);

            totalOutQty = totalOutQty.add(outQty);
            totalOutCost = totalOutCost.add(fifoCost);
        }

        // 按日期排序所有流水
        movements.sort((a, b) -> {
            LocalDate dateA = (LocalDate) a.get("date");
            LocalDate dateB = (LocalDate) b.get("date");
            if (dateA == null) dateA = LocalDate.MIN;
            if (dateB == null) dateB = LocalDate.MIN;
            return dateA.compareTo(dateB);
        });

        // 计算当前库存价值（剩余批次）
        BigDecimal currentQty = BigDecimal.ZERO;
        BigDecimal currentValue = BigDecimal.ZERO;
        for (StockInItem item : allInItems) {
            BigDecimal remain = remainingQty.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (remain.compareTo(BigDecimal.ZERO) > 0) {
                currentQty = currentQty.add(remain);
                BigDecimal up = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                currentValue = currentValue.add(remain.multiply(up));
            }
        }

        // 构建FIFO批次明细
        for (StockInItem item : allInItems) {
            BigDecimal remain = remainingQty.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (remain.compareTo(BigDecimal.ZERO) > 0) {
                Map<String, Object> batch = new HashMap<>();
                batch.put("batchId", item.getId());
                batch.put("stockInId", item.getStockInId());
                batch.put("date", stockInDates.get(item.getStockInId()));
                batch.put("originalQty", item.getQuantity());
                batch.put("remainingQty", remain);
                batch.put("unitPrice", item.getUnitPrice());
                BigDecimal bup = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                batch.put("value", remain.multiply(bup));
                fifoBatches.add(batch);
            }
        }

        // 汇总结果
        Map<String, Object> result = new HashMap<>();
        result.put("commodityId", commodityId);
        result.put("totalInQty", totalInQty);
        result.put("totalInCost", totalInCost);
        result.put("totalOutQty", totalOutQty);
        result.put("totalOutCost", totalOutCost);
        result.put("currentQty", currentQty);
        result.put("currentValue", currentValue);
        result.put("avgCost", currentQty.compareTo(BigDecimal.ZERO) > 0 ?
                currentValue.divide(currentQty, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        result.put("fifoBatches", fifoBatches);
        result.put("movements", movements);

        List<Map<String, Object>> resultList = new ArrayList<>();
        resultList.add(result);
        return resultList;
    }

    @Override
    public List<Map<String, Object>> getStocktakeSummary(String productType, String keyword) {
        LambdaQueryWrapper<Commodity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productType)) {
            wrapper.eq(Commodity::getProductType, productType);
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Commodity::getName, escaped)
                    .or().like(Commodity::getCode, escaped));
        }
        wrapper.orderByAsc(Commodity::getCode);
        List<Commodity> commodities = commodityMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Commodity c : commodities) {
            BigDecimal currentQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
            if (currentQty.compareTo(BigDecimal.ZERO) == 0) continue;

            // 计算FIFO成本
            BigDecimal fifoValue = calculateFIFOValue(c.getId());
            BigDecimal avgCost = currentQty.compareTo(BigDecimal.ZERO) > 0 ?
                    fifoValue.divide(currentQty, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            Map<String, Object> row = new HashMap<>();
            row.put("commodityId", c.getId());
            row.put("code", c.getCode());
            row.put("name", c.getName());
            row.put("unit", c.getUnit());
            row.put("currentQty", currentQty);
            row.put("fifoValue", fifoValue);
            row.put("avgCost", avgCost);
            row.put("costPrice", c.getCostPrice());
            result.add(row);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getStockSnapshot(String date, String productType, String keyword) {
        LocalDate snapshotDate = StringUtils.hasText(date) ? LocalDate.parse(date) : LocalDate.now();

        LambdaQueryWrapper<Commodity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productType)) {
            wrapper.eq(Commodity::getProductType, productType);
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Commodity::getName, escaped)
                    .or().like(Commodity::getCode, escaped));
        }
        wrapper.orderByAsc(Commodity::getCode);
        List<Commodity> commodities = commodityMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Commodity c : commodities) {
            // 计算指定日期的库存快照
            BigDecimal openingQty = calculateOpeningQty(c.getId(), snapshotDate);
            BigDecimal inQty = calculatePeriodIn(c.getId(), snapshotDate, LocalDate.now());
            BigDecimal outQty = calculatePeriodOut(c.getId(), snapshotDate, LocalDate.now());
            BigDecimal closingQty = openingQty.add(inQty).subtract(outQty);

            Map<String, Object> row = new HashMap<>();
            row.put("commodityId", c.getId());
            row.put("code", c.getCode());
            row.put("name", c.getName());
            row.put("unit", c.getUnit());
            row.put("snapshotDate", snapshotDate);
            row.put("openingQty", openingQty);
            row.put("inQty", inQty);
            row.put("outQty", outQty);
            row.put("closingQty", closingQty);
            result.add(row);
        }

        return result;
    }

    private BigDecimal calculateFIFOValue(Integer commodityId) {
        // 获取入库批次
        LambdaQueryWrapper<StockInItem> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(StockInItem::getCommodityId, commodityId);
        List<StockInItem> inItems = stockInItemMapper.selectList(inWrapper);

        // 获取出库总量
        LambdaQueryWrapper<StockOutItem> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(StockOutItem::getCommodityId, commodityId);
        List<StockOutItem> outItems = stockOutItemMapper.selectList(outWrapper);
        BigDecimal totalOut = outItems.stream()
                .map(item -> item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // FIFO计算剩余库存价值
        BigDecimal remaining = totalOut;
        BigDecimal value = BigDecimal.ZERO;

        for (StockInItem item : inItems) {
            BigDecimal itemQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal itemPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                // 已全部出库，剩余都是库存
                value = value.add(itemQty.multiply(itemPrice));
            } else {
                BigDecimal available = itemQty;
                BigDecimal deduct = remaining.min(available);
                BigDecimal leftover = available.subtract(deduct);
                remaining = remaining.subtract(deduct);
                if (leftover.compareTo(BigDecimal.ZERO) > 0) {
                    value = value.add(leftover.multiply(itemPrice));
                }
            }
        }

        return value;
    }

    private BigDecimal calculateOpeningQty(Integer commodityId, LocalDate date) {
        // 期初 = 当前库存 - 日期之后的入库 + 日期之后的出库
        Commodity c = commodityMapper.selectById(commodityId);
        BigDecimal currentQty = c != null && c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
        BigDecimal afterIn = stockInItemMapper.sumQuantityByCommodityAndDateRange(commodityId, date, null);
        BigDecimal afterOut = stockOutItemMapper.sumQuantityByCommodityAndDateRange(commodityId, date, null);
        return currentQty.subtract(afterIn != null ? afterIn : BigDecimal.ZERO)
                .add(afterOut != null ? afterOut : BigDecimal.ZERO);
    }

    private BigDecimal calculatePeriodIn(Integer commodityId, LocalDate startDate, LocalDate endDate) {
        BigDecimal qty = stockInItemMapper.sumQuantityByCommodityAndDateRange(commodityId, startDate, endDate);
        return qty != null ? qty : BigDecimal.ZERO;
    }

    private BigDecimal calculatePeriodOut(Integer commodityId, LocalDate startDate, LocalDate endDate) {
        BigDecimal qty = stockOutItemMapper.sumQuantityByCommodityAndDateRange(commodityId, startDate, endDate);
        return qty != null ? qty : BigDecimal.ZERO;
    }

    @Override
    public List<Map<String, Object>> getStockMovements(Integer commodityId, String startDate, String endDate,
                                                        String productType, String keyword) {
        LocalDate start = StringUtils.hasText(startDate) ? LocalDate.parse(startDate) : null;
        LocalDate end = StringUtils.hasText(endDate) ? LocalDate.parse(endDate) : null;

        // 获取商品列表
        LambdaQueryWrapper<Commodity> commodityWrapper = new LambdaQueryWrapper<>();
        if (commodityId != null) {
            commodityWrapper.eq(Commodity::getId, commodityId);
        }
        if (StringUtils.hasText(productType)) {
            commodityWrapper.eq(Commodity::getProductType, productType);
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            commodityWrapper.and(w -> w.like(Commodity::getName, escaped)
                    .or().like(Commodity::getCode, escaped));
        }
        List<Commodity> commodities = commodityMapper.selectList(commodityWrapper);

        List<Map<String, Object>> allMovements = new ArrayList<>();

        for (Commodity c : commodities) {
            // 获取入库记录
            LambdaQueryWrapper<StockInItem> inItemWrapper = new LambdaQueryWrapper<>();
            inItemWrapper.eq(StockInItem::getCommodityId, c.getId());
            List<StockInItem> inItems = stockInItemMapper.selectList(inItemWrapper);

            // 获取入库单日期
            Map<Integer, LocalDate> stockInDates = new HashMap<>();
            Map<Integer, String> stockInOrderNos = new HashMap<>();
            Set<Integer> stockInIds = inItems.stream()
                    .map(StockInItem::getStockInId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Integer stockInId : stockInIds) {
                StockIn stockIn = stockInMapper.selectById(stockInId);
                if (stockIn != null) {
                    stockInDates.put(stockInId, stockIn.getOrderDate());
                    stockInOrderNos.put(stockInId, stockIn.getOrderNo());
                }
            }

            // 获取出库记录
            LambdaQueryWrapper<StockOutItem> outItemWrapper = new LambdaQueryWrapper<>();
            outItemWrapper.eq(StockOutItem::getCommodityId, c.getId());
            List<StockOutItem> outItems = stockOutItemMapper.selectList(outItemWrapper);

            // 获取出库单日期
            Map<Integer, LocalDate> stockOutDates = new HashMap<>();
            Map<Integer, String> stockOutOrderNos = new HashMap<>();
            Set<Integer> stockOutIds = outItems.stream()
                    .map(StockOutItem::getStockOutId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Integer stockOutId : stockOutIds) {
                StockOut stockOut = stockOutMapper.selectById(stockOutId);
                if (stockOut != null) {
                    stockOutDates.put(stockOutId, stockOut.getOrderDate());
                    stockOutOrderNos.put(stockOutId, stockOut.getOrderNo());
                }
            }

            // 添加入库记录
            for (StockInItem item : inItems) {
                LocalDate date = stockInDates.get(item.getStockInId());
                if (start != null && date != null && date.isBefore(start)) continue;
                if (end != null && date != null && date.isAfter(end)) continue;

                Map<String, Object> movement = new HashMap<>();
                movement.put("commodityId", c.getId());
                movement.put("commodityCode", c.getCode());
                movement.put("commodityName", c.getName());
                movement.put("unit", c.getUnit());
                movement.put("type", "in");
                movement.put("orderNo", stockInOrderNos.get(item.getStockInId()));
                movement.put("date", date);
                movement.put("quantity", item.getQuantity());
                movement.put("unitPrice", item.getUnitPrice());
                BigDecimal iq = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal ip = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                movement.put("amount", iq.multiply(ip));
                allMovements.add(movement);
            }

            // 添加出库记录
            for (StockOutItem item : outItems) {
                LocalDate date = stockOutDates.get(item.getStockOutId());
                if (start != null && date != null && date.isBefore(start)) continue;
                if (end != null && date != null && date.isAfter(end)) continue;

                Map<String, Object> movement = new HashMap<>();
                movement.put("commodityId", c.getId());
                movement.put("commodityCode", c.getCode());
                movement.put("commodityName", c.getName());
                movement.put("unit", c.getUnit());
                movement.put("type", "out");
                movement.put("orderNo", stockOutOrderNos.get(item.getStockOutId()));
                movement.put("date", date);
                movement.put("quantity", item.getQuantity());
                movement.put("unitPrice", item.getUnitPrice());
                BigDecimal oq = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal op = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                movement.put("amount", oq.multiply(op));
                allMovements.add(movement);
            }
        }

        // 按日期排序
        allMovements.sort((a, b) -> {
            LocalDate dateA = (LocalDate) a.get("date");
            LocalDate dateB = (LocalDate) b.get("date");
            if (dateA == null) dateA = LocalDate.MIN;
            if (dateB == null) dateB = LocalDate.MIN;
            return dateA.compareTo(dateB);
        });

        return allMovements;
    }
}
