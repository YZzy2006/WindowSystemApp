package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("after_sale_order")
public class AfterSaleOrder extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private Integer orderId;
    private String saleOrderNo;
    private Integer customerId;
    private String customerName;
    private String type = "repair";
    private String source = "phone";
    private String description;
    private String status = "pending";
    private String assignedTo;
    private LocalDate scheduledDate;
    private LocalDate completedDate;
    private String resolution;
    private BigDecimal cost = BigDecimal.ZERO;
    private Integer isWarranty = 0;
    private String remark;
    private String images;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
