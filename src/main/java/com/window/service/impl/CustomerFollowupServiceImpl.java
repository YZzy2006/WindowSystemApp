package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.CustomerFollowup;
import com.window.mapper.CustomerFollowupMapper;
import com.window.service.CustomerFollowupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerFollowupServiceImpl implements CustomerFollowupService {

    private final CustomerFollowupMapper customerFollowupMapper;

    @Override
    public IPage<CustomerFollowup> listByCustomerId(Integer customerId, Page<CustomerFollowup> page) {
        LambdaQueryWrapper<CustomerFollowup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerFollowup::getCustomerId, customerId)
               .orderByDesc(CustomerFollowup::getCreateTime);
        return customerFollowupMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(CustomerFollowup followup) {
        customerFollowupMapper.insert(followup);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        customerFollowupMapper.deleteById(id);
    }
}
