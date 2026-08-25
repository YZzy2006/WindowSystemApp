package com.window.dto;

import lombok.Data;

@Data
public class EnquiryUpdateDto {
    private Boolean isRead;
    private Integer isMeasured;
    private Integer isStarred;
    private Integer isCompleted;
    private String remark;
}
