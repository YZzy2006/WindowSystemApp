package com.window.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseOrderSaveDto {
    private String orderNo;
    private Integer supplierId;
    private String supplierName;
    private String supplierContact;
    private String supplierPhone;
    private String supplierAddress;
    private LocalDate orderDate;
    private String status;
    private String remark;
    private List<PurchaseOrderItemDto> items;

    @Data
    public static class PurchaseOrderItemDto {
        private Integer commodityId;
        private String productName;
        private String productCategory;
        private String spec;
        private String unit;
        private String warehouseLoc;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private String remark;
    }
}
