package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("print_setting")
public class PrintSetting {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String type;
    private String config;
    private LocalDateTime updateTime;
}
