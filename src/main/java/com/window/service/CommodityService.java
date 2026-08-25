package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.Commodity;

import java.util.List;
import java.util.Map;

public interface CommodityService {
    IPage<Commodity> list(Page<Commodity> page, String productType, String pricingRule, String unit, String keyword);
    Commodity getById(Integer id);
    void save(Commodity commodity);
    void updateById(Commodity commodity);
    void removeById(Integer id);
    void toggleShow(Integer id);
    List<Map<String, Object>> getAlerts();
    List<Map<String, Object>> getInventoryByDateRange(String startDate, String endDate, String productType, String keyword);
}
