package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.SaleReturn;
import com.window.entity.SaleReturnItem;

import java.util.List;
import java.util.Map;

public interface SaleReturnService {
    IPage<SaleReturn> list(Page<SaleReturn> page, String keyword, String status, String startDate, String endDate);
    Map<String, Object> getDetail(Integer id);
    SaleReturn getById(Integer id);
    void save(SaleReturn saleReturn, List<SaleReturnItem> items);
    void updateById(SaleReturn saleReturn, List<SaleReturnItem> items);
    void removeById(Integer id);
    void updateStatus(Integer id, String status);
    void toggleCleared(Integer id);
    Map<String, Object> getSummary(String startDate, String endDate);
    Map<Integer, List<SaleReturnItem>> getItemsByOrderIds(List<Integer> orderIds);
    Map<String, Object> getAnalysis(String startDate, String endDate);
}
