package com.window.controller;

import com.window.dto.Result;
import com.window.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public Result overview(@RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate) {
        return Result.success(dashboardService.getOverview(startDate, endDate));
    }

    @GetMapping("/customer-rank")
    public Result customerRank(@RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        return Result.success(dashboardService.getCustomerRank(startDate, endDate));
    }

    @GetMapping("/product-rank")
    public Result productRank(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate) {
        return Result.success(dashboardService.getProductRank(startDate, endDate));
    }

    @GetMapping("/receivable-aging")
    public Result receivableAging(@RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        return Result.success(dashboardService.getReceivableAging(startDate, endDate));
    }

    @GetMapping("/receivable-aging/details")
    public Result receivableAgingDetails(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return Result.success(dashboardService.getReceivableAgingDetails(startDate, endDate, page, size));
    }

    @GetMapping("/customer-orders")
    public Result customerOrders(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate) {
        return Result.success(dashboardService.getCustomerOrders(keyword, startDate, endDate));
    }

    @GetMapping("/reminders")
    public Result reminders() {
        return Result.success(dashboardService.getReminders());
    }
}
