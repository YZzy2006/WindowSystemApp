package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sale_order_item")
public class SaleOrderItem extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private Integer commodityId;
    private String productName;
    private String series;
    private String color;
    private String productType;
    private String unit;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal wallThickness;
    private String glassType;
    private String lockPosition;
    private Integer doorCount;
    private BigDecimal fangCount;
    private Integer diaojiao;
    private Integer taoCount;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal cost;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal accessoryCost;
    private Long formulaId;
    private BigDecimal extraFee;
    private Boolean manualArea;
    private Boolean fangPending;
    private Boolean doorPending;
    private String formulaSnapshot;
    private String image;
    private String remark;
    private Integer sort = 0;
    private LocalDateTime createTime;
}
