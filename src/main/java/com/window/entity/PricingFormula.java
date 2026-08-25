package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pricing_formula")
public class PricingFormula extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "公式名称不能为空")
    @Size(max = 100, message = "公式名称不能超过100个字符")
    private String name;
    private String unit;
    @NotBlank(message = "公式表达式不能为空")
    private String formula;
    private String parameters;
    private String description;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
