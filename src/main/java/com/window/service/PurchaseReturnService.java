package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.PurchaseReturn;
import com.window.entity.PurchaseReturnItem;

import java.util.List;
import java.util.Map;

public interface PurchaseReturnService {
    IPage<PurchaseReturn> list(Page<PurchaseReturn> page, String keyword, String status, String startDate, String endDate);
    Map<String, Object> getDetail(Integer id);
    PurchaseReturn getById(Integer id);
    void save(PurchaseReturn purchaseReturn, List<PurchaseReturnItem> items);
    void updateById(PurchaseReturn purchaseReturn, List<PurchaseReturnItem> items);
    void removeById(Integer id);
    void updateStatus(Integer id, String status);
    void toggleCleared(Integer id);
    Map<String, Object> getSummary(String startDate, String endDate);
    Map<Integer, List<PurchaseReturnItem>> getItemsByOrderIds(List<Integer> orderIds);
    Map<String, Object> getAnalysis(String startDate, String endDate);
}
