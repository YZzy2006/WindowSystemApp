package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.PurchaseOrder;
import com.window.entity.PurchaseReturn;
import com.window.entity.Supplier;
import com.window.mapper.PurchaseOrderMapper;
import com.window.mapper.PurchaseReturnMapper;
import com.window.mapper.SupplierMapper;
import com.window.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierMapper supplierMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;

    @Override
    public IPage<Supplier> list(Page<Supplier> page, String keyword) {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(Supplier::getName, escaped)
                    .or().like(Supplier::getPhone, escaped)
                    .or().like(Supplier::getContact, escaped));
        }
        wrapper.orderByDesc(Supplier::getIsStarred).orderByDesc(Supplier::getCreateTime);
        return supplierMapper.selectPage(page, wrapper);
    }

    @Override
    public Supplier getById(Integer id) {
        return supplierMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Supplier supplier) {
        supplierMapper.insert(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(Supplier supplier) {
        supplierMapper.updateById(supplier);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        // 检查是否被采购单引用
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseOrder::getSupplierId, id);
        if (purchaseOrderMapper.selectCount(wrapper) > 0) {
            throw new IllegalArgumentException("该供应商已被采购单引用，无法删除");
        }
        // 检查是否被采购退货单引用
        LambdaQueryWrapper<PurchaseReturn> retWrapper = new LambdaQueryWrapper<>();
        retWrapper.eq(PurchaseReturn::getSupplierId, id);
        if (purchaseReturnMapper.selectCount(retWrapper) > 0) {
            throw new IllegalArgumentException("该供应商已被采购退货单引用，无法删除");
        }
        supplierMapper.deleteById(id);
    }
}
