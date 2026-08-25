package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private Integer afterSaleId;
    private String orderNo;
    @NotNull(message = "收付款日期不能为空")
    private LocalDate paymentDate;
    @NotBlank(message = "收付款类型不能为空")
    private String type;
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private BigDecimal amount;
    @Size(max = 100, message = "对方名称不能超过100个字符")
    private String partyName;
    private String remark;
    private String orderType;
    private LocalDateTime createTime;
    @TableField(exist = false)
    private Integer orderCleared;
}
