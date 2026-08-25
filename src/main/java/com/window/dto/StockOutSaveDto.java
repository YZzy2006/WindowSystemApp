package com.window.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StockOutSaveDto {
    private String orderNo;
    private LocalDate orderDate;
    private String applicant;
    private String warehouseKeeper;
    private String operator;
    private String remark;
    private List<StockOutItemDto> items;

    @Data
    public static class StockOutItemDto {
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
