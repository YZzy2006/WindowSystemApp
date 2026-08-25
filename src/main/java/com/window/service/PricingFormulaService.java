package com.window.service;

import com.window.entity.PricingFormula;

import java.util.List;

public interface PricingFormulaService {
    List<PricingFormula> list();
    PricingFormula getById(Long id);
    void save(PricingFormula formula);
    void updateById(PricingFormula formula);
    void removeById(Long id);
}
