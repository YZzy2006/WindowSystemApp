package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("order_sequence")
public class OrderSequence {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String seqType;
    private String prefix;
    private Integer currentSeq = 1;
    private LocalDate lastDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
