package com.window.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AfterSaleOrderSaveDto {
    private Integer orderId;
    private String saleOrderNo;
    private Integer customerId;
    private String customerName;
    private String type;
    private String source;
    private String description;
    private String status;
    private String assignedTo;
    private String technician;
    private String scheduledDate;
    private String appointmentDate;
    private String completedDate;
    private String resolution;
    private BigDecimal cost;
    private Integer isWarranty;
    private String remark;
    private String images;
}
