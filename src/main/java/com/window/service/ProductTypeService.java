package com.window.service;

import com.window.entity.ProductType;

import java.util.List;

public interface ProductTypeService {
    List<ProductType> list();
    ProductType getById(Integer id);
    void save(ProductType productType);
    void updateById(ProductType productType);
    void removeById(Integer id);
}
