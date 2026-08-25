package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sale_return")
public class SaleReturn extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private String originalOrderNo;
    private Integer customerId;
    private String customerName;
    private LocalDate returnDate;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private Integer isCleared = 0;
    private String status = "pending";
    private String remark;
    private LocalDateTime createTime;
}
