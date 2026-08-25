package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stock_in_item")
public class StockInItem extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer stockInId;
    private Integer commodityId;
    private String productName;
    private String productCategory;
    private String spec;
    private String unit;
    private String warehouseLoc;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String remark;
    private LocalDateTime createTime;
}
