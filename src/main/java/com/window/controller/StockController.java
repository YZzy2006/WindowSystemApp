package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.dto.StockInSaveDto;
import com.window.dto.StockOutSaveDto;
import com.window.entity.StockIn;
import com.window.entity.StockInItem;
import com.window.entity.StockOut;
import com.window.entity.StockOutItem;
import com.window.service.StockInService;
import com.window.service.StockOutService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/stock")
@RequiredArgsConstructor
public class StockController {
    private final StockInService stockInService;
    private final StockOutService stockOutService;

    @GetMapping("/in")
    public Result listIn(@RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size,
                         @RequestParam(required = false) String keyword,
                         @RequestParam(required = false) String startDate,
                         @RequestParam(required = false) String endDate) {
        return Result.success(stockInService.list(new Page<>(page, size), keyword, startDate, endDate));
    }

    @GetMapping("/in/{id}")
    public Result getInDetail(@PathVariable Integer id) {
        Map<String, Object> detail = stockInService.getDetail(id);
        if (detail == null) return Result.error(404, "入库单不存在");
        return Result.success(detail);
    }

    @PostMapping("/in")
    public Result saveIn(@RequestBody StockInSaveDto body) {
        StockIn stockIn = toStockIn(body);
        List<StockInItem> items = toStockInItems(body.getItems());
        stockInService.save(stockIn, items);
        return Result.success("添加成功");
    }

    @PutMapping("/in/{id}")
    public Result updateIn(@PathVariable Integer id, @RequestBody StockInSaveDto body) {
        StockIn stockIn = toStockIn(body);
        stockIn.setId(id);
        List<StockInItem> items = toStockInItems(body.getItems());
        stockInService.updateById(stockIn, items);
        return Result.success("修改成功");
    }

    @DeleteMapping("/in/{id}")
    public Result deleteIn(@PathVariable Integer id) {
        stockInService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/in/summary")
    public Result stockInSummary(@RequestParam(required = false) String startDate,
                                 @RequestParam(required = false) String endDate) {
        return Result.success(stockInService.getSummary(startDate, endDate));
    }

    @GetMapping("/out")
    public Result listOut(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate) {
        return Result.success(stockOutService.list(new Page<>(page, size), keyword, startDate, endDate));
    }

    @GetMapping("/out/{id}")
    public Result getOutDetail(@PathVariable Integer id) {
        Map<String, Object> detail = stockOutService.getDetail(id);
        if (detail == null) return Result.error(404, "出库单不存在");
        return Result.success(detail);
    }

    @PostMapping("/out")
    public Result saveOut(@RequestBody StockOutSaveDto body) {
        StockOut stockOut = toStockOut(body);
        List<StockOutItem> items = toStockOutItems(body.getItems());
        stockOutService.save(stockOut, items);
        return Result.success("添加成功");
    }

    @PutMapping("/out/{id}")
    public Result updateOut(@PathVariable Integer id, @RequestBody StockOutSaveDto body) {
        StockOut stockOut = toStockOut(body);
        stockOut.setId(id);
        List<StockOutItem> items = toStockOutItems(body.getItems());
        stockOutService.updateById(stockOut, items);
        return Result.success("修改成功");
    }

    @DeleteMapping("/out/{id}")
    public Result deleteOut(@PathVariable Integer id) {
        stockOutService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/out/summary")
    public Result stockOutSummary(@RequestParam(required = false) String startDate,
                                  @RequestParam(required = false) String endDate) {
        return Result.success(stockOutService.getSummary(startDate, endDate));
    }

    private StockIn toStockIn(StockInSaveDto dto) {
        StockIn stockIn = new StockIn();
        stockIn.setOrderNo(dto.getOrderNo());
        stockIn.setOrderDate(dto.getOrderDate());
        stockIn.setApplicant(dto.getApplicant());
        stockIn.setWarehouseKeeper(dto.getWarehouseKeeper());
        stockIn.setOperator(dto.getOperator());
        stockIn.setRemark(dto.getRemark());
        return stockIn;
    }

    private List<StockInItem> toStockInItems(List<StockInSaveDto.StockInItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        return dtoList.stream().map(dto -> {
            StockInItem item = new StockInItem();
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

    private StockOut toStockOut(StockOutSaveDto dto) {
        StockOut stockOut = new StockOut();
        stockOut.setOrderNo(dto.getOrderNo());
        stockOut.setOrderDate(dto.getOrderDate());
        stockOut.setApplicant(dto.getApplicant());
        stockOut.setWarehouseKeeper(dto.getWarehouseKeeper());
        stockOut.setOperator(dto.getOperator());
        stockOut.setRemark(dto.getRemark());
        return stockOut;
    }

    private List<StockOutItem> toStockOutItems(List<StockOutSaveDto.StockOutItemDto> dtoList) {
        if (dtoList == null) return new ArrayList<>();
        return dtoList.stream().map(dto -> {
            StockOutItem item = new StockOutItem();
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
