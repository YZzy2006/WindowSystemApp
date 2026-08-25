// com/window/service/impl/ProductServiceImpl.java
package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.Product;
import com.window.mapper.ProductMapper;
import com.window.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Product> listByCategory(Integer categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getIsShow, 1);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        return productMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> listAll() {
        return productMapper.selectList(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getById(Integer id) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getId, id).eq(Product::getIsShow, 1);
        return productMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Product product) {
        productMapper.insert(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Product product) {
        productMapper.updateById(product);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        productMapper.deleteById(id);
    }

}
