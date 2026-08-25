package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("enquiry")
public class Enquiry extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "电话不能为空")
    private String phone;

    private String content;

    private String remark;

    private Integer isRead = 0;
    private Integer needMeasure = 0;   // 是否需要上门量尺 0否 1是
    private String budget;             // 预算区间
    private Integer isMeasured = 0;    // 是否已量尺 0否 1是
    private Integer isStarred = 0;     // 是否星标 0否 1是
    private Integer isCompleted = 0;   // 是否完单 0否 1是
    private LocalDateTime createTime;

}
