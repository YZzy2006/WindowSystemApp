package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.ProductType;
import com.window.mapper.ProductTypeMapper;
import com.window.service.ProductTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductTypeServiceImpl implements ProductTypeService {
    private final ProductTypeMapper productTypeMapper;

    @Override
    public List<ProductType> list() {
        LambdaQueryWrapper<ProductType> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(ProductType::getSort).orderByDesc(ProductType::getCreateTime);
        return productTypeMapper.selectList(wrapper);
    }

    @Override
    public ProductType getById(Integer id) {
        return productTypeMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(ProductType productType) {
        productTypeMapper.insert(productType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(ProductType productType) {
        productTypeMapper.updateById(productType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        productTypeMapper.deleteById(id);
    }
}
