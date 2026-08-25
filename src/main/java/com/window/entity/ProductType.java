package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_type")
public class ProductType extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Integer id;
    @NotBlank(message = "细目名称不能为空")
    @Size(max = 50, message = "细目名称不能超过50个字符")
    private String name;
    private String summaryDesc;
    private Integer sort;
    private LocalDateTime createTime;
}
