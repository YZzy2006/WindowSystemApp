package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.*;
import com.window.mapper.*;
import com.window.service.CommodityService;
import com.window.exception.OrderNoExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CommodityServiceImpl implements CommodityService {
    private final CommodityMapper commodityMapper;
    private final CommodityCategoryMapper categoryMapper;
    private final StockInItemMapper stockInItemMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SaleReturnItemMapper saleReturnItemMapper;
    private final PurchaseReturnItemMapper purchaseReturnItemMapper;
    private final PricingFormulaMapper pricingFormulaMapper;

    @Override
    public IPage<Commodity> list(Page<Commodity> page, String productType, String pricingRule, String unit, String keyword) {
        LambdaQueryWrapper<Commodity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(productType)) {
            wrapper.eq(Commodity::getProductType, productType);
        }
        if (StringUtils.hasText(pricingRule)) {
            // 同时匹配 pricingRule 字段或对应单位（兼容旧数据）
            wrapper.and(w -> {
                w.eq(Commodity::getPricingRule, pricingRule);
                switch (pricingRule) {
                    case "area" -> w.or().in(Commodity::getUnit, "方", "sqm");
                    case "count" -> w.or().eq(Commodity::getUnit, "套");
                    case "linear" -> w.or().eq(Commodity::getUnit, "米");
                    case "height" -> w.or().eq(Commodity::getUnit, "吊脚");
                }
            });
        }
        if (StringUtils.hasText(unit)) {
            wrapper.eq(Commodity::getUnit, unit);
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Commodity::getName, escaped)
                    .or().like(Commodity::getCode, escaped));
        }
        wrapper.orderByDesc(Commodity::getCreateTime);
        return commodityMapper.selectPage(page, wrapper);
    }

    @Override
    public Commodity getById(Integer id) {
        return commodityMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Commodity commodity) {
        // 编码唯一预检（含软删除行，唯一索引仍占用）
        if (StringUtils.hasText(commodity.getCode()) && commodityMapper.countByCode(commodity.getCode()) > 0) {
            throw new OrderNoExistsException("编码已存在：" + commodity.getCode());
        }
        resolveFormulaId(commodity);
        commodityMapper.insert(commodity);
    }

    /** 商品必须绑公式：优先按导入的公式名解析，其次按单位绑定默认公式（sort 最小者） */
    private void resolveFormulaId(Commodity commodity) {
        if (commodity.getFormulaId() != null) return;
        // 1. 按公式名称（导入字段）
        if (StringUtils.hasText(commodity.getFormulaName())) {
            LambdaQueryWrapper<PricingFormula> nw = new LambdaQueryWrapper<>();
            nw.eq(PricingFormula::getName, commodity.getFormulaName()).last("LIMIT 1");
            PricingFormula byName = pricingFormulaMapper.selectOne(nw);
            if (byName != null) {
                commodity.setFormulaId(byName.getId());
                return;
            }
        }
        // 2. 按单位默认公式
        if (StringUtils.hasText(commodity.getUnit())) {
            LambdaQueryWrapper<PricingFormula> uw = new LambdaQueryWrapper<>();
            uw.eq(PricingFormula::getUnit, commodity.getUnit()).orderByAsc(PricingFormula::getSort).orderByAsc(PricingFormula::getId).last("LIMIT 1");
            PricingFormula byUnit = pricingFormulaMapper.selectOne(uw);
            if (byUnit != null) {
                commodity.setFormulaId(byUnit.getId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Commodity commodity) {
        resolveFormulaId(commodity);
        commodityMapper.updateById(commodity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        // 检查是否被入库明细引用
        LambdaQueryWrapper<StockInItem> inWrapper = new LambdaQueryWrapper<>();
        inWrapper.eq(StockInItem::getCommodityId, id);
        if (stockInItemMapper.selectCount(inWrapper) > 0) {
            throw new IllegalArgumentException("该商品已被入库单引用，无法删除");
        }
        // 检查是否被出库明细引用
        LambdaQueryWrapper<StockOutItem> outWrapper = new LambdaQueryWrapper<>();
        outWrapper.eq(StockOutItem::getCommodityId, id);
        if (stockOutItemMapper.selectCount(outWrapper) > 0) {
            throw new IllegalArgumentException("该商品已被出库单引用，无法删除");
        }
        // 检查是否被销售单明细引用
        LambdaQueryWrapper<SaleOrderItem> soiWrapper = new LambdaQueryWrapper<>();
        soiWrapper.eq(SaleOrderItem::getCommodityId, id);
        if (saleOrderItemMapper.selectCount(soiWrapper) > 0) {
            throw new IllegalArgumentException("该商品已被销售单引用，无法删除");
        }
        // 检查是否被采购单明细引用
        LambdaQueryWrapper<PurchaseOrderItem> poiWrapper = new LambdaQueryWrapper<>();
        poiWrapper.eq(PurchaseOrderItem::getCommodityId, id);
        if (purchaseOrderItemMapper.selectCount(poiWrapper) > 0) {
            throw new IllegalArgumentException("该商品已被采购单引用，无法删除");
        }
        // 检查是否被销售退货明细引用
        LambdaQueryWrapper<SaleReturnItem> sriWrapper = new LambdaQueryWrapper<>();
        sriWrapper.eq(SaleReturnItem::getCommodityId, id);
        if (saleReturnItemMapper.selectCount(sriWrapper) > 0) {
            throw new IllegalArgumentException("该商品已被销售退货单引用，无法删除");
        }
        // 检查是否被采购退货明细引用
        LambdaQueryWrapper<PurchaseReturnItem> priWrapper = new LambdaQueryWrapper<>();
        priWrapper.eq(PurchaseReturnItem::getCommodityId, id);
        if (purchaseReturnItemMapper.selectCount(priWrapper) > 0) {
            throw new IllegalArgumentException("该商品已被采购退货单引用，无法删除");
        }
        commodityMapper.deleteById(id);
    }

    @Override
    public void toggleShow(Integer id) {
        LambdaUpdateWrapper<Commodity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Commodity::getId, id)
                .setSql("is_show = 1 - is_show");
        commodityMapper.update(null, wrapper);
    }

    @Override
    public List<Map<String, Object>> getAlerts() {
        LambdaQueryWrapper<Commodity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Commodity::getIsShow, 1);
        List<Commodity> commodities = commodityMapper.selectList(wrapper);

        // Load category names
        Map<Integer, String> categoryMap = new HashMap<>();
        List<CommodityCategory> categories = categoryMapper.selectList(null);
        for (CommodityCategory cat : categories) {
            categoryMap.put(cat.getId(), cat.getName());
        }

        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Commodity c : commodities) {
            BigDecimal qty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
            String status = "正常";
            if (c.getAlertLow() != null && qty.compareTo(c.getAlertLow()) <= 0) {
                status = "严重不足";
            } else if (c.getAlertMid() != null && qty.compareTo(c.getAlertMid()) <= 0) {
                status = "库存偏低";
            } else if (c.getAlertHigh() != null && qty.compareTo(c.getAlertHigh()) >= 0) {
                status = "库存过高";
            }
            if (!"正常".equals(status)) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("commodityId", c.getId());
                alert.put("code", c.getCode());
                alert.put("name", c.getName());
                alert.put("unit", c.getUnit());
                alert.put("currentQty", c.getCurrentQty());
                alert.put("alertHigh", c.getAlertHigh());
                alert.put("alertMid", c.getAlertMid());
                alert.put("alertLow", c.getAlertLow());
                alert.put("alertStatus", status);
                alerts.add(alert);
            }
        }

        // Sort by severity: 严重不足 > 库存偏低 > 库存过高
        alerts.sort((a, b) -> {
            Map<String, Integer> order = Map.of("严重不足", 0, "库存偏低", 1, "库存过高", 2);
            return order.getOrDefault(a.get("alertStatus"), 3)
                    .compareTo(order.getOrDefault(b.get("alertStatus"), 3));
        });

        return alerts;
    }

    @Override
    public List<Map<String, Object>> getInventoryByDateRange(String startDate, String endDate, String productType, String keyword) {
        // 查询商品列表
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

        LocalDate start = StringUtils.hasText(startDate) ? LocalDate.parse(startDate) : null;
        LocalDate end = StringUtils.hasText(endDate) ? LocalDate.parse(endDate) : null;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Commodity c : commodities) {
            // 使用SQL直接统计日期范围内的出入库
            BigDecimal inQty = stockInItemMapper.sumQuantityByCommodityAndDateRange(c.getId(), start, end);
            BigDecimal outQty = stockOutItemMapper.sumQuantityByCommodityAndDateRange(c.getId(), start, end);
            if (inQty == null) inQty = BigDecimal.ZERO;
            if (outQty == null) outQty = BigDecimal.ZERO;

            // 期末库存 = 当前库存（commodity表的currentQty）
            BigDecimal closingQty = c.getCurrentQty() != null ? c.getCurrentQty() : BigDecimal.ZERO;
            // 期初库存 = 期末 - 本期入库 + 本期出库
            BigDecimal openingQty = closingQty.subtract(inQty).add(outQty);

            Map<String, Object> row = new HashMap<>();
            row.put("commodityId", c.getId());
            row.put("code", c.getCode());
            row.put("name", c.getName());
            row.put("unit", c.getUnit());
            row.put("openingQty", openingQty);
            row.put("inQty", inQty);
            row.put("outQty", outQty);
            row.put("closingQty", closingQty);
            result.add(row);
        }

        return result;
    }
}
