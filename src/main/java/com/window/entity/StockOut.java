package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_out")
public class StockOut extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private LocalDate orderDate;
    private String applicant;
    private String warehouseKeeper;
    private String operator;
    private String remark;
    private LocalDateTime createTime;
}
