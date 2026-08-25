package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.window.entity.OrderSequence;
import com.window.entity.SysConfig;
import com.window.mapper.OrderSequenceMapper;
import com.window.mapper.SysConfigMapper;
import com.window.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {
    private final SysConfigMapper sysConfigMapper;
    private final OrderSequenceMapper orderSequenceMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        List<SysConfig> configs = sysConfigMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (SysConfig config : configs) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    public String get(String key) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig config = sysConfigMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void set(String key, String value, String remark) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigKey, key);
        SysConfig existing = sysConfigMapper.selectOne(wrapper);
        if (existing != null) {
            existing.setConfigValue(value);
            if (remark != null) {
                existing.setRemark(remark);
            }
            sysConfigMapper.updateById(existing);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setRemark(remark);
            sysConfigMapper.insert(config);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetSequences() {
        List<OrderSequence> sequences = orderSequenceMapper.selectList(null);
        for (OrderSequence seq : sequences) {
            seq.setCurrentSeq(1);
            orderSequenceMapper.updateById(seq);
        }
    }
}
