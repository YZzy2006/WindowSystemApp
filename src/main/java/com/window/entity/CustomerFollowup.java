package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer_followup")
public class CustomerFollowup {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer customerId;
    private Integer adminId;
    private String username;
    private String content;
    private LocalDateTime createTime;
}
