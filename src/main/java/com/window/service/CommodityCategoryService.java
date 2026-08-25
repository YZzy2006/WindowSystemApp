package com.window.service;

import com.window.entity.CommodityCategory;

import java.util.List;

public interface CommodityCategoryService {
    List<CommodityCategory> list();
    CommodityCategory getById(Integer id);
    void save(CommodityCategory category);
    void updateById(CommodityCategory category);
    void removeById(Integer id);
}
