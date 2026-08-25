package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.dto.SaleOrderSaveDto;
import com.window.entity.SaleOrder;
import com.window.entity.SaleOrderItem;
import com.window.service.SaleOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sale-orders")
@RequiredArgsConstructor
public class SaleOrderController {
    private final SaleOrderService saleOrderService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) Integer isCleared,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate) {
        if (size > 100) size = 100;
        if (page < 1) page = 1;
        return Result.success(saleOrderService.list(new Page<>(page, size), keyword, status, isCleared, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result getDetail(@PathVariable Integer id) {
        Map<String, Object> detail = saleOrderService.getDetail(id);
        if (detail == null) return Result.error(404, "订单不存在");
        return Result.success(detail);
    }

    @PostMapping
    public Result save(@Valid @RequestBody SaleOrderSaveDto body) {
        SaleOrder order = toOrder(body);
        List<SaleOrderItem> items = toItems(body.getItems());
        saleOrderService.save(order, items);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @Valid @RequestBody SaleOrderSaveDto body) {
        SaleOrder order = toOrder(body);
        order.setId(id);
        List<SaleOrderItem> items = toItems(body.getItems());
        saleOrderService.updateById(order, items);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        saleOrderService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/status")
    public Result updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (!StringUtils.hasText(status)) {
            return Result.error(400, "状态不能为空");
        }
        saleOrderService.updateStatus(id, status);
        return Result.success("状态更新成功");
    }

    @PutMapping("/{id}/toggle-cleared")
    public Result toggleCleared(@PathVariable Integer id) {
        saleOrderService.toggleCleared(id);
        return Result.success("已更新");
    }

    @PostMapping("/batch/delete")
    public Result batchDelete(@RequestBody Map<String, List<Integer>> body) {
        List<Integer> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) return Result.error(400, "请选择要删除的订单");
        if (ids.size() > 500) return Result.error(400, "单次批量删除不能超过500个订单");
        saleOrderService.batchDelete(ids);
        return Result.success("批量删除成功");
    }

    @PutMapping("/batch/status")
    public Result batchUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String status = (String) body.get("status");
        if (ids == null || ids.isEmpty()) return Result.error(400, "请选择订单");
        if (!StringUtils.hasText(status)) return Result.error(400, "状态不能为空");
        saleOrderService.batchUpdateStatus(ids, status);
        return Result.success("批量状态更新成功");
    }

    @GetMapping("/items/batch")
    public Result getItemsBatch(@RequestParam List<Integer> orderIds) {
        if (orderIds.size() > 200) {
            return Result.error(400, "单次查询不能超过200个订单");
        }
        return Result.success(saleOrderService.getItemsByOrderIds(orderIds));
    }

    @GetMapping("/summary")
    public Result summary(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Integer isCleared,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate) {
        return Result.success(saleOrderService.getSummary(keyword, status, isCleared, startDate, endDate));
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
        return Result.success(saleOrderService.getReport(startDate, endDate, keyword));
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private SaleOrder toOrder(SaleOrderSaveDto dto) {
        SaleOrder order = new SaleOrder();
        order.setOrderNo(dto.getOrderNo());
        order.setOrderType(dto.getOrderType());
        order.setCustomerId(dto.getCustomerId());
        order.setCustomerName(dto.getCustomerName());
        order.setCustomerPhone(dto.getCustomerPhone());
        order.setCustomerAddress(dto.getCustomerAddress());
        order.setOrderDate(dto.getOrderDate());
        order.setDeposit(dto.getDeposit());
        order.setStatus(dto.getStatus());
        order.setRemark(dto.getRemark());
        order.setNotice(dto.getNotice());
        order.setHiddenProductTypes(dto.getHiddenProductTypes());
        return order;
    }

    private List<SaleOrderItem> toItems(List<SaleOrderSaveDto.SaleOrderItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        return dtoList.stream().map(dto -> {
            SaleOrderItem item = new SaleOrderItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setSeries(dto.getSeries());
            item.setColor(dto.getColor());
            item.setProductType(dto.getProductType());
            item.setUnit(dto.getUnit());
            item.setWidth(dto.getWidth());
            item.setHeight(dto.getHeight());
            item.setWallThickness(dto.getWallThickness());
            item.setGlassType(dto.getGlassType());
            item.setLockPosition(dto.getLockPosition());
            item.setDoorCount(dto.getDoorCount());
            item.setDiaojiao(dto.getDiaojiao());
            item.setFangCount(dto.getFangCount());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setCost(dto.getCost());
            item.setMaterialCost(dto.getMaterialCost());
            item.setLaborCost(dto.getLaborCost());
            item.setAccessoryCost(dto.getAccessoryCost());
            item.setExtraFee(dto.getExtraFee());
            item.setAmount(dto.getAmount());
            item.setManualArea(dto.getManualArea());
            item.setFangPending(dto.getFangPending());
            item.setDoorPending(dto.getDoorPending());
            item.setFormulaId(dto.getFormulaId());
            item.setFormulaSnapshot(dto.getFormulaSnapshot());
            item.setImage(dto.getImage());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(java.util.stream.Collectors.toList());
    }
}
