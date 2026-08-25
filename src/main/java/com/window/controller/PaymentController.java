package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.entity.Payment;
import com.window.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping("/payments")
    public Result listPayments(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Integer orderId,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        return Result.success(paymentService.list(new Page<>(page, size), orderId, type, keyword, startDate, endDate));
    }

    @GetMapping("/payments/{id}")
    public Result getPayment(@PathVariable Integer id) {
        Payment payment = paymentService.getById(id);
        if (payment == null) return Result.error(404, "记录不存在");
        return Result.success(payment);
    }

    @PostMapping("/payments")
    public Result savePayment(@Valid @RequestBody Payment payment) {
        paymentService.save(payment);
        return Result.success("添加成功");
    }

    @PutMapping("/payments/{id}")
    public Result updatePayment(@PathVariable Integer id, @Valid @RequestBody Payment payment) {
        payment.setId(id);
        paymentService.updateById(payment);
        return Result.success("修改成功");
    }

    @DeleteMapping("/payments/{id}")
    public Result deletePayment(@PathVariable Integer id) {
        paymentService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/payments/summary")
    public Result paymentSummary(@RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) String type,
                                 @RequestParam(required = false) String keyword) {
        return Result.success(paymentService.getSummary(startDate, endDate, type, keyword));
    }

    @GetMapping("/payments/unpaid")
    public Result unpaidOrders(@RequestParam(required = false) String keyword) {
        return Result.success(paymentService.getUnpaidOrders(keyword));
    }

    @GetMapping("/payments/report")
    public Result paymentReport(@RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate,
                                @RequestParam(required = false) String keyword) {
        if (StringUtils.hasText(startDate) && !isValidDate(startDate)) {
            return Result.error(400, "起始日期格式不正确，请使用 YYYY-MM-DD 格式");
        }
        if (StringUtils.hasText(endDate) && !isValidDate(endDate)) {
            return Result.error(400, "截止日期格式不正确，请使用 YYYY-MM-DD 格式");
        }
        return Result.success(paymentService.getPaymentReport(startDate, endDate, keyword));
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
