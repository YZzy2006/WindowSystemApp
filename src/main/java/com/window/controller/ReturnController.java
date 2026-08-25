package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.PurchaseReturnSaveDto;
import com.window.dto.Result;
import com.window.dto.SaleReturnSaveDto;
import com.window.entity.PurchaseReturn;
import com.window.entity.PurchaseReturnItem;
import com.window.entity.SaleReturn;
import com.window.entity.SaleReturnItem;
import com.window.service.PurchaseReturnService;
import com.window.service.SaleReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/returns")
@RequiredArgsConstructor
public class ReturnController {
    private final SaleReturnService saleReturnService;
    private final PurchaseReturnService purchaseReturnService;

    @GetMapping("/sale")
    public Result listSaleReturns(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        return Result.success(saleReturnService.list(new Page<>(page, size), keyword, status, startDate, endDate));
    }

    @GetMapping("/sale/{id}")
    public Result getSaleReturnDetail(@PathVariable Integer id) {
        Map<String, Object> detail = saleReturnService.getDetail(id);
        if (detail == null) return Result.error(404, "退货单不存在");
        return Result.success(detail);
    }

    @PostMapping("/sale")
    public Result saveSaleReturn(@RequestBody SaleReturnSaveDto body) {
        SaleReturn saleReturn = toSaleReturn(body);
        List<SaleReturnItem> items = toSaleReturnItems(body.getItems());
        saleReturnService.save(saleReturn, items);
        return Result.success("添加成功");
    }

    @PutMapping("/sale/{id}")
    public Result updateSaleReturn(@PathVariable Integer id, @RequestBody SaleReturnSaveDto body) {
        SaleReturn saleReturn = toSaleReturn(body);
        saleReturn.setId(id);
        List<SaleReturnItem> items = toSaleReturnItems(body.getItems());
        saleReturnService.updateById(saleReturn, items);
        return Result.success("修改成功");
    }

    @DeleteMapping("/sale/{id}")
    public Result deleteSaleReturn(@PathVariable Integer id) {
        saleReturnService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/sale/{id}/status")
    public Result updateSaleReturnStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        saleReturnService.updateStatus(id, body.get("status"));
        return Result.success("状态更新成功");
    }

    @GetMapping("/sale/items/batch")
    public Result getSaleReturnItemsBatch(@RequestParam List<Integer> orderIds) {
        return Result.success(saleReturnService.getItemsByOrderIds(orderIds));
    }

    @GetMapping("/sale/summary")
    public Result saleReturnSummary(@RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate) {
        return Result.success(saleReturnService.getSummary(startDate, endDate));
    }

    @GetMapping("/sale/analysis")
    public Result saleReturnAnalysis(@RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        return Result.success(saleReturnService.getAnalysis(startDate, endDate));
    }

    @GetMapping("/purchase")
    public Result listPurchaseReturns(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String startDate,
                                      @RequestParam(required = false) String endDate) {
        return Result.success(purchaseReturnService.list(new Page<>(page, size), keyword, status, startDate, endDate));
    }

    @GetMapping("/purchase/{id}")
    public Result getPurchaseReturnDetail(@PathVariable Integer id) {
        Map<String, Object> detail = purchaseReturnService.getDetail(id);
        if (detail == null) return Result.error(404, "退货单不存在");
        return Result.success(detail);
    }

    @PostMapping("/purchase")
    public Result savePurchaseReturn(@RequestBody PurchaseReturnSaveDto body) {
        PurchaseReturn purchaseReturn = toPurchaseReturn(body);
        List<PurchaseReturnItem> items = toPurchaseReturnItems(body.getItems());
        purchaseReturnService.save(purchaseReturn, items);
        return Result.success("添加成功");
    }

    @PutMapping("/purchase/{id}")
    public Result updatePurchaseReturn(@PathVariable Integer id, @RequestBody PurchaseReturnSaveDto body) {
        PurchaseReturn purchaseReturn = toPurchaseReturn(body);
        purchaseReturn.setId(id);
        List<PurchaseReturnItem> items = toPurchaseReturnItems(body.getItems());
        purchaseReturnService.updateById(purchaseReturn, items);
        return Result.success("修改成功");
    }

    @DeleteMapping("/purchase/{id}")
    public Result deletePurchaseReturn(@PathVariable Integer id) {
        purchaseReturnService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/purchase/{id}/status")
    public Result updatePurchaseReturnStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        purchaseReturnService.updateStatus(id, body.get("status"));
        return Result.success("状态更新成功");
    }

    @PutMapping("/purchase/{id}/toggle-cleared")
    public Result togglePurchaseReturnCleared(@PathVariable Integer id) {
        purchaseReturnService.toggleCleared(id);
        return Result.success("已更新");
    }

    @PutMapping("/sale/{id}/toggle-cleared")
    public Result toggleSaleReturnCleared(@PathVariable Integer id) {
        saleReturnService.toggleCleared(id);
        return Result.success("已更新");
    }

    @GetMapping("/purchase/items/batch")
    public Result getPurchaseReturnItemsBatch(@RequestParam List<Integer> orderIds) {
        return Result.success(purchaseReturnService.getItemsByOrderIds(orderIds));
    }

    @GetMapping("/purchase/summary")
    public Result purchaseReturnSummary(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate) {
        return Result.success(purchaseReturnService.getSummary(startDate, endDate));
    }

    @GetMapping("/purchase/analysis")
    public Result purchaseReturnAnalysis(@RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        return Result.success(purchaseReturnService.getAnalysis(startDate, endDate));
    }

    private SaleReturn toSaleReturn(SaleReturnSaveDto dto) {
        SaleReturn sr = new SaleReturn();
        sr.setOrderNo(dto.getOrderNo());
        sr.setOriginalOrderNo(dto.getOriginalOrderNo());
        sr.setCustomerId(dto.getCustomerId());
        sr.setCustomerName(dto.getCustomerName());
        sr.setReturnDate(dto.getReturnDate());
        sr.setStatus(dto.getStatus());
        sr.setRemark(dto.getRemark());
        return sr;
    }

    private List<SaleReturnItem> toSaleReturnItems(List<SaleReturnSaveDto.SaleReturnItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        return dtoList.stream().map(dto -> {
            SaleReturnItem item = new SaleReturnItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }

    private PurchaseReturn toPurchaseReturn(PurchaseReturnSaveDto dto) {
        PurchaseReturn pr = new PurchaseReturn();
        pr.setOrderNo(dto.getOrderNo());
        pr.setOriginalOrderNo(dto.getOriginalOrderNo());
        pr.setSupplierId(dto.getSupplierId());
        pr.setSupplierName(dto.getSupplierName());
        pr.setReturnDate(dto.getReturnDate());
        pr.setStatus(dto.getStatus());
        pr.setRemark(dto.getRemark());
        return pr;
    }

    private List<PurchaseReturnItem> toPurchaseReturnItems(List<PurchaseReturnSaveDto.PurchaseReturnItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        return dtoList.stream().map(dto -> {
            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }
}
