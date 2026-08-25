package com.window.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaleOrderSaveDto {
    private String orderNo;
    private String orderType;
    private Integer customerId;
    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称不能超过100个字符")
    private String customerName;
    @Size(max = 20, message = "联系电话不能超过20个字符")
    private String customerPhone;
    @Size(max = 200, message = "客户地址不能超过200个字符")
    private String customerAddress;
    @NotNull(message = "订单日期不能为空")
    private LocalDate orderDate;
    private BigDecimal deposit;
    private String status;
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
    @Size(max = 500, message = "通知不能超过500个字符")
    private String notice;
    private String hiddenProductTypes;
    @Valid
    private List<SaleOrderItemDto> items;

    @Data
    public static class SaleOrderItemDto {
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
        private Integer diaojiao;
        private BigDecimal fangCount;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal cost;
        private BigDecimal materialCost;
        private BigDecimal laborCost;
        private BigDecimal accessoryCost;
        private BigDecimal extraFee;
        /** 手改金额（砍价）：用户点击金额列手动编辑后传入；非空时后端保留该金额，不再按公式重算覆盖 */
        private BigDecimal amount;
        private Long formulaId;
        private String formulaSnapshot;
        private String image;
        private Boolean manualArea;
        private Boolean fangPending;
        private Boolean doorPending;
        private String remark;
    }
}
