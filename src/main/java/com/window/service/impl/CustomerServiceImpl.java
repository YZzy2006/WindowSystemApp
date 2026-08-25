package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.AfterSaleOrder;
import com.window.entity.Customer;
import com.window.entity.SaleOrder;
import com.window.entity.SaleReturn;
import com.window.mapper.AfterSaleOrderMapper;
import com.window.mapper.CustomerMapper;
import com.window.mapper.SaleOrderMapper;
import com.window.mapper.SaleReturnMapper;
import com.window.service.CustomerService;
import com.window.exception.OrderNoExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
    private final CustomerMapper customerMapper;
    private final SaleOrderMapper saleOrderMapper;
    private final SaleReturnMapper saleReturnMapper;
    private final AfterSaleOrderMapper afterSaleOrderMapper;

    @Override
    public IPage<Customer> list(Page<Customer> page, String keyword) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Customer::getName, escaped)
                    .or().like(Customer::getPhone, escaped)
                    .or().like(Customer::getContact, escaped));
        }
        wrapper.orderByDesc(Customer::getIsStarred).orderByDesc(Customer::getCreateTime);
        return customerMapper.selectPage(page, wrapper);
    }

    @Override
    public Customer getById(Integer id) {
        return customerMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Customer customer) {
        // 名称唯一预检（含软删除行，唯一索引仍占用）
        if (StringUtils.hasText(customer.getName()) && customerMapper.countByName(customer.getName()) > 0) {
            throw new OrderNoExistsException("名称已存在：" + customer.getName());
        }
        customerMapper.insert(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Customer customer) {
        customerMapper.updateById(customer);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        // 检查是否被销售单引用
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrder::getCustomerId, id);
        if (saleOrderMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("该客户已被销售单引用，无法删除");
        }
        // 检查是否被销售退货单引用
        LambdaQueryWrapper<SaleReturn> retWrapper = new LambdaQueryWrapper<>();
        retWrapper.eq(SaleReturn::getCustomerId, id);
        if (saleReturnMapper.selectCount(retWrapper) > 0) {
            throw new IllegalArgumentException("该客户已被销售退货单引用，无法删除");
        }
        // 检查是否被售后单引用
        LambdaQueryWrapper<AfterSaleOrder> asWrapper = new LambdaQueryWrapper<>();
        asWrapper.eq(AfterSaleOrder::getCustomerId, id);
        if (afterSaleOrderMapper.selectCount(asWrapper) > 0) {
            throw new IllegalArgumentException("该客户已被售后单引用，无法删除");
        }
        customerMapper.deleteById(id);
    }
}
