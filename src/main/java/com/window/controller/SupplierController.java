package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.entity.Supplier;
import com.window.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService supplierService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword) {
        return Result.success(supplierService.list(new Page<>(page, size), keyword));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Supplier supplier = supplierService.getById(id);
        if (supplier == null) return Result.error(404, "供应商不存在");
        return Result.success(supplier);
    }

    @PostMapping
    public Result save(@Valid @RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @Valid @RequestBody Supplier supplier) {
        supplier.setId(id);
        supplierService.updateById(supplier);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        supplierService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/star")
    public Result toggleStar(@PathVariable Integer id) {
        Supplier supplier = supplierService.getById(id);
        if (supplier == null) return Result.error(404, "供应商不存在");
        supplier.setIsStarred(supplier.getIsStarred() == 1 ? 0 : 1);
        supplierService.updateById(supplier);
        return Result.success(supplier.getIsStarred() == 1 ? "已标星" : "已取消星标");
    }
}
