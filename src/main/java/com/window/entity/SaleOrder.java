package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sale_order")
public class SaleOrder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private String orderType = "sale";
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private LocalDate orderDate;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal deposit = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private Integer isCleared = 0;
    private String status = "pending";
    private String remark;
    private String notice;
    private BigDecimal windowArea = BigDecimal.ZERO;
    private Integer windowCount = 0;
    private BigDecimal doorArea = BigDecimal.ZERO;
    private Integer doorCount = 0;
    private String hiddenProductTypes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
