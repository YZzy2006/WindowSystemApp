package com.window.service;

import java.util.List;
import java.util.Map;

public interface DashboardService {
    Map<String, Object> getOverview(String startDate, String endDate);
    List<Map<String, Object>> getCustomerRank(String startDate, String endDate);
    List<Map<String, Object>> getProductRank(String startDate, String endDate);
    List<Map<String, Object>> getReceivableAging(String startDate, String endDate);
    Map<String, Object> getReceivableAgingDetails(String startDate, String endDate, int page, int size);
    Map<String, Object> getCustomerOrders(String keyword, String startDate, String endDate);
    Map<String, Object> getReminders();
}
