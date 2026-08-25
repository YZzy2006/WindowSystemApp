package com.window.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.window.entity.PrintSetting;
import com.window.mapper.PrintSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrintSettingService extends ServiceImpl<PrintSettingMapper, PrintSetting> {

    public PrintSetting getSetting() {
        return getSetting("order");
    }

    public PrintSetting getSetting(String type) {
        LambdaQueryWrapper<PrintSetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PrintSetting::getType, type).last("LIMIT 1");
        return getOne(wrapper);
    }

    public void saveSetting(String config) {
        saveSetting("order", config);
    }

    public void saveSetting(String type, String config) {
        PrintSetting setting = getSetting(type);
        if (setting == null) {
            setting = new PrintSetting();
            setting.setType(type);
            setting.setConfig(config);
            save(setting);
        } else {
            setting.setConfig(config);
            updateById(setting);
        }
    }
}
