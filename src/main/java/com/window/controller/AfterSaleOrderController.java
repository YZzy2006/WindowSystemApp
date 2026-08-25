package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.AfterSaleOrderSaveDto;
import com.window.dto.Result;
import com.window.entity.AfterSaleOrder;
import com.window.service.AfterSaleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/after-sale")
@RequiredArgsConstructor
public class AfterSaleOrderController {
    private final AfterSaleOrderService afterSaleOrderService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String type,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        return Result.success(afterSaleOrderService.list(new Page<>(page, size), keyword, status, type, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        AfterSaleOrder order = afterSaleOrderService.getById(id);
        if (order == null) return Result.error(404, "售后单不存在");
        return Result.success(order);
    }

    @PostMapping
    public Result save(@RequestBody AfterSaleOrderSaveDto body) {
        AfterSaleOrder order = toOrder(body);
        afterSaleOrderService.save(order);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody AfterSaleOrderSaveDto body) {
        AfterSaleOrder order = toOrder(body);
        order.setId(id);
        afterSaleOrderService.updateById(order);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        afterSaleOrderService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        afterSaleOrderService.updateStatus(id, body.get("status"));
        return Result.success("状态更新成功");
    }

    @GetMapping("/summary")
    public Result summary(@RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate) {
        return Result.success(afterSaleOrderService.getSummary(startDate, endDate));
    }

    private AfterSaleOrder toOrder(AfterSaleOrderSaveDto dto) {
        AfterSaleOrder order = new AfterSaleOrder();
        order.setOrderId(dto.getOrderId());
        order.setSaleOrderNo(dto.getSaleOrderNo());
        order.setCustomerId(dto.getCustomerId());
        order.setCustomerName(dto.getCustomerName());
        order.setType(dto.getType());
        order.setSource(dto.getSource());
        order.setDescription(dto.getDescription());
        order.setStatus(dto.getStatus());
        String assignedTo = dto.getAssignedTo();
        if (assignedTo == null) assignedTo = dto.getTechnician();
        order.setAssignedTo(assignedTo);
        String scheduledDateStr = dto.getScheduledDate();
        if (scheduledDateStr == null) scheduledDateStr = dto.getAppointmentDate();
        if (scheduledDateStr != null && !scheduledDateStr.isEmpty()) {
            order.setScheduledDate(LocalDate.parse(scheduledDateStr));
        }
        if (dto.getCompletedDate() != null && !dto.getCompletedDate().isEmpty()) {
            order.setCompletedDate(LocalDate.parse(dto.getCompletedDate()));
        }
        order.setResolution(dto.getResolution());
        order.setCost(dto.getCost());
        order.setIsWarranty(dto.getIsWarranty());
        order.setRemark(dto.getRemark());
        order.setImages(dto.getImages());
        return order;
    }
}
