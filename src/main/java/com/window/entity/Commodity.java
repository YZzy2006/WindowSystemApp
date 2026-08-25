package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("commodity")
public class Commodity extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "商品编码不能为空")
    @Size(max = 50, message = "商品编码不能超过50个字符")
    private String code;
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200, message = "商品名称不能超过200个字符")
    private String name;
    private String material;
    private String productType;
    private String unit;
    private BigDecimal defaultPrice;
    private BigDecimal costPrice;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal accessoryCost;
    private String pricingRule;
    private Long formulaId;
    private String warehouseLoc;
    private BigDecimal initialQty;
    private BigDecimal initialCost;
    private BigDecimal currentQty = BigDecimal.ZERO;
    private String remark;
    private BigDecimal alertHigh;
    private BigDecimal alertMid;
    private BigDecimal alertLow;
    private Integer isShow = 1;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    /** 别名（JSON 数组字符串，如 ["别名1","别名2"]） */
    private String aliases;
    /** 导入用：按公式名称解析绑定 formulaId（不落库） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String formulaName;
}
