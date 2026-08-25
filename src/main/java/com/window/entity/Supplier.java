package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supplier")
public class Supplier extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 100, message = "供应商名称不能超过100个字符")
    private String name;
    @Size(max = 50, message = "联系人不能超过50个字符")
    private String contact;
    private String phone;
    private String address;
    private String remark;
    @TableField("is_starred")
    private Integer isStarred = 0;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
