package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.StockIn;
import com.window.entity.StockInItem;

import java.util.List;
import java.util.Map;

public interface StockInService {
    IPage<StockIn> list(Page<StockIn> page, String keyword, String startDate, String endDate);
    Map<String, Object> getDetail(Integer id);
    StockIn getById(Integer id);
    void save(StockIn stockIn, List<StockInItem> items);
    void updateById(StockIn stockIn, List<StockInItem> items);
    void removeById(Integer id);
    Map<String, Object> getSummary(String startDate, String endDate);
}
