package com.window.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.CustomerFollowup;

public interface CustomerFollowupService {

    IPage<CustomerFollowup> listByCustomerId(Integer customerId, Page<CustomerFollowup> page);

    void save(CustomerFollowup followup);

    void removeById(Integer id);
}
