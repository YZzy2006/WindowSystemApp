package com.window.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseReturnSaveDto {
    private String orderNo;
    private String originalOrderNo;
    private Integer supplierId;
    private String supplierName;
    private LocalDate returnDate;
    private String status;
    private String remark;
    private List<PurchaseReturnItemDto> items;

    @Data
    public static class PurchaseReturnItemDto {
        private Integer commodityId;
        private String productName;
        private String productCategory;
        private String spec;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String remark;
    }
}
