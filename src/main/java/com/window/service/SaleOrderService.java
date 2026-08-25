package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.SaleOrder;
import com.window.entity.SaleOrderItem;

import java.util.List;
import java.util.Map;

public interface SaleOrderService {
    IPage<SaleOrder> list(Page<SaleOrder> page, String keyword, String status, Integer isCleared, String startDate, String endDate);
    Map<String, Object> getDetail(Integer id);
    SaleOrder getById(Integer id);
    void save(SaleOrder order, List<SaleOrderItem> items);
    void updateById(SaleOrder order, List<SaleOrderItem> items);
    void removeById(Integer id);
    void updateStatus(Integer id, String status);
    void toggleCleared(Integer id);
    void batchDelete(List<Integer> ids);
    void batchUpdateStatus(List<Integer> ids, String status);
    Map<String, Object> getSummary(String keyword, String status, Integer isCleared, String startDate, String endDate);
    Map<Integer, List<SaleOrderItem>> getItemsByOrderIds(List<Integer> orderIds);
    Map<String, Object> getReport(String startDate, String endDate, String keyword);
}
