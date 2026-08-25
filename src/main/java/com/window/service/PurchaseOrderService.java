package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.PurchaseOrder;
import com.window.entity.PurchaseOrderItem;

import java.util.List;
import java.util.Map;

public interface PurchaseOrderService {
    IPage<PurchaseOrder> list(Page<PurchaseOrder> page, String keyword, String status, String startDate, String endDate);
    Map<String, Object> getDetail(Integer id);
    PurchaseOrder getById(Integer id);
    void save(PurchaseOrder order, List<PurchaseOrderItem> items);
    void updateById(PurchaseOrder order, List<PurchaseOrderItem> items);
    void removeById(Integer id);
    void updateStatus(Integer id, String status);
    void toggleCleared(Integer id);
    Map<String, Object> getSummary(String startDate, String endDate);
    Map<Integer, List<PurchaseOrderItem>> getItemsByOrderIds(List<Integer> orderIds);
    Map<String, Object> getReport(String startDate, String endDate, String keyword);
}
