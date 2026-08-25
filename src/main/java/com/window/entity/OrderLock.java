package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_lock")
public class OrderLock {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderType;
    private Integer orderId;
    private Integer adminId;
    private String username;
    private LocalDateTime lockTime;
}
