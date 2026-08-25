package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("purchase_order")
public class PurchaseOrder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private Integer supplierId;
    private String supplierName;
    private String supplierContact;
    private String supplierPhone;
    private String supplierAddress;
    private LocalDate orderDate;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private Integer isCleared = 0;
    private String status = "pending";
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
