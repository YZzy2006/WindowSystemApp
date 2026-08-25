package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.Supplier;

public interface SupplierService {
    IPage<Supplier> list(Page<Supplier> page, String keyword);
    Supplier getById(Integer id);
    void save(Supplier supplier);
    void updateById(Supplier supplier);
    void removeById(Integer id);
}
