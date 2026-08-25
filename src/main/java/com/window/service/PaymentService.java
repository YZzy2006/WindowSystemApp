package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.Payment;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    IPage<Payment> list(Page<Payment> page, Integer orderId, String type, String keyword, String startDate, String endDate);
    Payment getById(Integer id);
    void save(Payment payment);
    void updateById(Payment payment);
    void removeById(Integer id);
    Map<String, Object> getSummary(String startDate, String endDate, String type, String keyword);
    List<Map<String, Object>> getUnpaidOrders(String keyword);
    Map<String, Object> getPaymentReport(String startDate, String endDate, String keyword);
}
