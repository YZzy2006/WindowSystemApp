package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @NotNull(message = "请选择分类")
    private Integer categoryId;

    @NotBlank(message = "产品名称不能为空")
    private String name;

    private String model;
    private String material;
    private BigDecimal price;
    private String coverImage;
    private String description;
    private String images;
    private String specs;
    private String priceTag;
    private String priceBreakdown;
    private String pricingRule;
    private String referencePrice;
    private String optionPrices;
    private String performance;
    private Integer isShow = 1;
    private LocalDateTime createTime;

}
