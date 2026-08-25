package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.StockOut;
import com.window.entity.StockOutItem;

import java.util.List;
import java.util.Map;

public interface StockOutService {
    IPage<StockOut> list(Page<StockOut> page, String keyword, String startDate, String endDate);
    Map<String, Object> getDetail(Integer id);
    StockOut getById(Integer id);
    void save(StockOut stockOut, List<StockOutItem> items);
    void updateById(StockOut stockOut, List<StockOutItem> items);
    void removeById(Integer id);
    Map<String, Object> getSummary(String startDate, String endDate);
}
