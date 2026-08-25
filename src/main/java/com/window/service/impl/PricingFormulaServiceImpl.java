package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.Commodity;
import com.window.entity.PricingFormula;
import com.window.mapper.CommodityMapper;
import com.window.mapper.PricingFormulaMapper;
import com.window.service.PricingFormulaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingFormulaServiceImpl implements PricingFormulaService {
    private final PricingFormulaMapper formulaMapper;
    private final CommodityMapper commodityMapper;

    @Override
    public List<PricingFormula> list() {
        LambdaQueryWrapper<PricingFormula> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(PricingFormula::getSort).orderByDesc(PricingFormula::getCreateTime);
        return formulaMapper.selectList(wrapper);
    }

    @Override
    public PricingFormula getById(Long id) {
        return formulaMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(PricingFormula formula) {
        formulaMapper.insert(formula);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(PricingFormula formula) {
        formulaMapper.updateById(formula);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long id) {
        LambdaQueryWrapper<Commodity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Commodity::getFormulaId, id);
        if (commodityMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("该定价公式已被商品引用，无法删除");
        }
        formulaMapper.deleteById(id);
    }
}
