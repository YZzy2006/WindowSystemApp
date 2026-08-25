package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.entity.Commodity;
import com.window.entity.CommodityCategory;
import com.window.entity.ProductType;
import com.window.service.CommodityCategoryService;
import com.window.service.CommodityService;
import com.window.service.ProductTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/commodities")
@RequiredArgsConstructor
public class CommodityController {
    private final CommodityService commodityService;
    private final CommodityCategoryService categoryService;
    private final ProductTypeService productTypeService;

    @GetMapping("/categories")
    public Result listCategories() {
        return Result.success(categoryService.list());
    }

    @PostMapping("/categories")
    public Result saveCategory(@Valid @RequestBody CommodityCategory category) {
        categoryService.save(category);
        return Result.success("添加成功");
    }

    @PutMapping("/categories/{id}")
    public Result updateCategory(@PathVariable Integer id, @Valid @RequestBody CommodityCategory category) {
        category.setId(id);
        categoryService.updateById(category);
        return Result.success("修改成功");
    }

    @DeleteMapping("/categories/{id}")
    public Result deleteCategory(@PathVariable Integer id) {
        categoryService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping("/product-types")
    public Result listProductTypes() {
        return Result.success(productTypeService.list());
    }

    @PostMapping("/product-types")
    public Result saveProductType(@Valid @RequestBody ProductType productType) {
        productTypeService.save(productType);
        return Result.success("添加成功");
    }

    @PutMapping("/product-types/{id}")
    public Result updateProductType(@PathVariable Integer id, @Valid @RequestBody ProductType productType) {
        productType.setId(id);
        productTypeService.updateById(productType);
        return Result.success("修改成功");
    }

    @DeleteMapping("/product-types/{id}")
    public Result deleteProductType(@PathVariable Integer id) {
        productTypeService.removeById(id);
        return Result.success("删除成功");
    }

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String productType,
                       @RequestParam(required = false) String pricingRule,
                       @RequestParam(required = false) String unit,
                       @RequestParam(required = false) String keyword) {
        return Result.success(commodityService.list(new Page<>(page, size), productType, pricingRule, unit, keyword));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Commodity commodity = commodityService.getById(id);
        if (commodity == null) return Result.error(404, "商品不存在");
        return Result.success(commodity);
    }

    @PostMapping
    public Result save(@Valid @RequestBody Commodity commodity) {
        commodityService.save(commodity);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @Valid @RequestBody Commodity commodity) {
        commodity.setId(id);
        commodityService.updateById(commodity);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        commodityService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/toggle")
    public Result toggleShow(@PathVariable Integer id) {
        commodityService.toggleShow(id);
        return Result.success("操作成功");
    }

    @GetMapping("/alerts")
    public Result getAlerts() {
        return Result.success(commodityService.getAlerts());
    }

    @GetMapping("/inventory")
    public Result getInventoryByDateRange(@RequestParam(required = false) String startDate,
                                          @RequestParam(required = false) String endDate,
                                          @RequestParam(required = false) String productType,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(commodityService.getInventoryByDateRange(startDate, endDate, productType, keyword));
    }
}
