package com.window.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class SaleReturnSaveDto {
    private String orderNo;
    private String originalOrderNo;
    private Integer customerId;
    private String customerName;
    private LocalDate returnDate;
    private String status;
    private String remark;
    private List<SaleReturnItemDto> items;

    @Data
    public static class SaleReturnItemDto {
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
