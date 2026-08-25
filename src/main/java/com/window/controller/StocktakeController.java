package com.window.controller;

import com.window.dto.Result;
import com.window.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stocktake")
@RequiredArgsConstructor
public class StocktakeController {
    private final StocktakeService stocktakeService;

    @GetMapping("/fifo/{commodityId}")
    public Result getFIFODetail(@PathVariable Integer commodityId) {
        return Result.success(stocktakeService.getFIFODetail(commodityId));
    }

    @GetMapping("/summary")
    public Result getStocktakeSummary(@RequestParam(required = false) String productType,
                                      @RequestParam(required = false) String keyword) {
        return Result.success(stocktakeService.getStocktakeSummary(productType, keyword));
    }

    @GetMapping("/snapshot")
    public Result getStockSnapshot(@RequestParam(required = false) String date,
                                   @RequestParam(required = false) String productType,
                                   @RequestParam(required = false) String keyword) {
        return Result.success(stocktakeService.getStockSnapshot(date, productType, keyword));
    }

    @GetMapping("/movements")
    public Result getStockMovements(@RequestParam(required = false) Integer commodityId,
                                    @RequestParam(required = false) String startDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false) String productType,
                                    @RequestParam(required = false) String keyword) {
        return Result.success(stocktakeService.getStockMovements(commodityId, startDate, endDate, productType, keyword));
    }
}
