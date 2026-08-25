package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer adminId;
    private String username;
    private String module;
    private String action;
    private String target;
    private String ip;
    private LocalDateTime createTime;
}
