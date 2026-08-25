// com/window/dto/EnquiryDto.java
package com.window.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class EnquiryDto {

    @NotBlank(message = "姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5a-zA-Z·]{2,20}$", message = "姓名格式不正确")
    private String name;

    @NotBlank(message = "电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的11位手机号")
    private String phone;

    @Pattern(regexp = "^[\\s\\S]{0,3000}$", message = "描述内容不能超过3000字")
    private String content;

    private Integer needMeasure;   // 是否需要上门量尺 0否 1是
    private String budget;         // 预算区间

}
