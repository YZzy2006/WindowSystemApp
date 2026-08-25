package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.Customer;

public interface CustomerService {
    IPage<Customer> list(Page<Customer> page, String keyword);
    Customer getById(Integer id);
    void save(Customer customer);
    void updateById(Customer customer);
    void removeById(Integer id);
}
