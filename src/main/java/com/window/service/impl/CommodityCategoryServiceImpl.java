package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.CommodityCategory;
import com.window.mapper.CommodityCategoryMapper;
import com.window.service.CommodityCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommodityCategoryServiceImpl implements CommodityCategoryService {
    private final CommodityCategoryMapper categoryMapper;

    @Override
    public List<CommodityCategory> list() {
        LambdaQueryWrapper<CommodityCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(CommodityCategory::getSort).orderByDesc(CommodityCategory::getCreateTime);
        return categoryMapper.selectList(wrapper);
    }

    @Override
    public CommodityCategory getById(Integer id) {
        return categoryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CommodityCategory category) {
        categoryMapper.insert(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(CommodityCategory category) {
        categoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        categoryMapper.deleteById(id);
    }
}
