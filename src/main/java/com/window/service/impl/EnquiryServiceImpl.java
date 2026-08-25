package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.window.entity.Enquiry;
import com.window.mapper.EnquiryMapper;
import com.window.service.EnquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnquiryServiceImpl implements EnquiryService {

    private final EnquiryMapper enquiryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Enquiry enquiry) {
        enquiryMapper.insert(enquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Enquiry> listAll() {
        LambdaQueryWrapper<Enquiry> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Enquiry::getCreateTime);
        return enquiryMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Integer id) {
        LambdaUpdateWrapper<Enquiry> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enquiry::getId, id).set(Enquiry::getIsRead, 1);
        enquiryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMeasured(Integer id, Integer measured) {
        LambdaUpdateWrapper<Enquiry> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enquiry::getId, id).set(Enquiry::getIsMeasured, measured);
        enquiryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markStarred(Integer id, Integer starred) {
        LambdaUpdateWrapper<Enquiry> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enquiry::getId, id).set(Enquiry::getIsStarred, starred);
        enquiryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markCompleted(Integer id, Integer completed) {
        LambdaUpdateWrapper<Enquiry> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enquiry::getId, id).set(Enquiry::getIsCompleted, completed);
        enquiryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead() {
        LambdaUpdateWrapper<Enquiry> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enquiry::getIsRead, 0).set(Enquiry::getIsRead, 1);
        enquiryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRemark(Integer id, String remark) {
        LambdaUpdateWrapper<Enquiry> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Enquiry::getId, id).set(Enquiry::getRemark, remark);
        enquiryMapper.update(null, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        enquiryMapper.deleteById(id);
    }

}
