package com.window.controller;

import com.window.dto.Result;
import com.window.entity.PricingFormula;
import com.window.service.PricingFormulaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/pricing-formulas")
@RequiredArgsConstructor
public class PricingFormulaController {
    private final PricingFormulaService formulaService;

    @GetMapping
    public Result list() {
        return Result.success(formulaService.list());
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        PricingFormula formula = formulaService.getById(id);
        if (formula == null) return Result.error(404, "公式不存在");
        return Result.success(formula);
    }

    @PostMapping
    public Result save(@Valid @RequestBody PricingFormula formula) {
        formulaService.save(formula);
        return Result.success("添加成功");
    }

    @PutMapping("/{id}")
    public Result update(@PathVariable Long id, @Valid @RequestBody PricingFormula formula) {
        formula.setId(id);
        formulaService.updateById(formula);
        return Result.success("修改成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        formulaService.removeById(id);
        return Result.success("删除成功");
    }
}
