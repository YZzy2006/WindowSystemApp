// com/window/service/impl/CaseServiceImpl.java
package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.Case;
import com.window.mapper.CaseMapper;
import com.window.service.CaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseServiceImpl implements CaseService {

    private final CaseMapper caseMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Case> listAll() {
        LambdaQueryWrapper<Case> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Case::getSort)
               .orderByDesc(Case::getCreateTime);
        return caseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Case> listVisible() {
        LambdaQueryWrapper<Case> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Case::getIsShow, 1)
               .orderByAsc(Case::getSort)
               .orderByDesc(Case::getCreateTime);
        return caseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(readOnly = true)
    public Case getById(Integer id) {
        return caseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Case c) {
        if (c.getIsShow() == null) c.setIsShow(1);
        caseMapper.insert(c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Case c) {
        caseMapper.updateById(c);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        caseMapper.deleteById(id);
    }

}
