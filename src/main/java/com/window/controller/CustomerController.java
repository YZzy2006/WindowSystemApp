package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.entity.Customer;
import com.window.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;

    @GetMapping
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String keyword) {
        return Result.success(customerService.list(new Page<>(page, size), keyword));
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Customer customer = customerService.getById(id);
        if (customer == null) return Result.error(404, "客户不存在");
        return Result.success(customer);
    }

    @PostMapping
    public Result save(@Valid @RequestBody Customer customer) {
        customerService.save(customer);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @Valid @RequestBody Customer customer) {
        customer.setId(id);
        customerService.updateById(customer);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        customerService.removeById(id);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/star")
    public Result toggleStar(@PathVariable Integer id) {
        Customer customer = customerService.getById(id);
        if (customer == null) return Result.error(404, "客户不存在");
        customer.setIsStarred(customer.getIsStarred() == 1 ? 0 : 1);
        customerService.updateById(customer);
        return Result.success(customer.getIsStarred() == 1 ? "已标星" : "已取消星标");
    }
}
