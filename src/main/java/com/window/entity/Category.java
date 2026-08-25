package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@TableName("category")
public class Category extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @NotBlank(message = "分类名称不能为空")
    private String name;

    private Integer sort = 0;

}
