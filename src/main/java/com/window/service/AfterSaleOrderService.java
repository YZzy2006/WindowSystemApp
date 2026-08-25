package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.AfterSaleOrder;

import java.util.Map;

public interface AfterSaleOrderService {
    IPage<AfterSaleOrder> list(Page<AfterSaleOrder> page, String keyword, String status, String type, String startDate, String endDate);
    AfterSaleOrder getById(Integer id);
    void save(AfterSaleOrder order);
    void updateById(AfterSaleOrder order);
    void removeById(Integer id);
    void updateStatus(Integer id, String status);
    Map<String, Object> getSummary(String startDate, String endDate);
}
