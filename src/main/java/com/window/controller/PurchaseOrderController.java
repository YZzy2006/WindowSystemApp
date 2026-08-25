package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.PurchaseOrderSaveDto;
import com.window.dto.Result;
import com.window.entity.PurchaseOrder;
import com.window.entity.PurchaseOrderItem;
import com.window.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        return Result.success(purchaseOrderService.list(new Page<>(page, size), keyword, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result getDetail(@PathVariable Integer id) {
        Map<String, Object> detail = purchaseOrderService.getDetail(id);
        if (detail == null) return Result.error(404, "采购单不存在");
        return Result.success(detail);
    }

    @PostMapping
    public Result save(@RequestBody PurchaseOrderSaveDto body) {
        PurchaseOrder order = toOrder(body);
        List<PurchaseOrderItem> items = toItems(body.getItems());
        purchaseOrderService.save(order, items);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody PurchaseOrderSaveDto body) {
        PurchaseOrder order = toOrder(body);
        order.setId(id);
        List<PurchaseOrderItem> items = toItems(body.getItems());
        purchaseOrderService.updateById(order, items);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        purchaseOrderService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        purchaseOrderService.updateStatus(id, body.get("status"));
        return Result.success("状态更新成功");
    }

    @PutMapping("/{id}/toggle-cleared")
    public Result toggleCleared(@PathVariable Integer id) {
        purchaseOrderService.toggleCleared(id);
        return Result.success("已更新");
    }

    @GetMapping("/items/batch")
    public Result getItemsBatch(@RequestParam List<Integer> orderIds) {
        return Result.success(purchaseOrderService.getItemsByOrderIds(orderIds));
    }

    @GetMapping("/summary")
    public Result getSummary(@RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        return Result.success(purchaseOrderService.getSummary(startDate, endDate));
    }

    @GetMapping("/report")
    public Result report(@RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate,
                         @RequestParam(required = false) String keyword) {
        if (StringUtils.hasText(startDate) && !isValidDate(startDate)) {
            return Result.error(400, "起始日期格式不正确，请使用 YYYY-MM-DD 格式");
        }
        if (StringUtils.hasText(endDate) && !isValidDate(endDate)) {
            return Result.error(400, "截止日期格式不正确，请使用 YYYY-MM-DD 格式");
        }
        return Result.success(purchaseOrderService.getReport(startDate, endDate, keyword));
    }

    private boolean isValidDate(String dateStr) {
        try { LocalDate.parse(dateStr); return true; } catch (Exception e) { return false; }
    }

    private PurchaseOrder toOrder(PurchaseOrderSaveDto dto) {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(dto.getOrderNo());
        order.setSupplierId(dto.getSupplierId());
        order.setSupplierName(dto.getSupplierName());
        order.setSupplierContact(dto.getSupplierContact());
        order.setSupplierPhone(dto.getSupplierPhone());
        order.setSupplierAddress(dto.getSupplierAddress());
        order.setOrderDate(dto.getOrderDate());
        order.setStatus(dto.getStatus());
        order.setRemark(dto.getRemark());
        return order;
    }

    private List<PurchaseOrderItem> toItems(List<PurchaseOrderSaveDto.PurchaseOrderItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        return dtoList.stream().map(dto -> {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setWarehouseLoc(dto.getWarehouseLoc());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }
}
