package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("case_info")
public class Case extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @NotBlank(message = "案例标题不能为空")
    private String title;

    private String image;
    private String images;       // 逗号分隔的图片URL，用于多图展示
    private String description;

    @TableField("product_id")
    private String productIds;      // 关联产品ID，逗号分隔，如 "100,101,102"
    private String location;        // 小区/位置
    private String productType;     // 产品类型标签
    private String profileSpec;     // 型材规格
    private String glassConfig;     // 玻璃配置
    private String hardwareBrand;   // 五金品牌
    private String beforeImage;     // 改造前封面（兼容旧数据）
    private String beforeImages;    // 改造前多图（逗号分隔）
    private String beforeDesc;      // 改造前描述文字
    private String afterDesc;       // 改造后描述文字
    private String customerNeed;    // 客户需求描述
    private String customerReview;  // 客户评价
    private String installDuration; // 安装时长

    private Integer sort;        // 排序值，越小越靠前
    private Integer isShow;      // 0隐藏 1展示

    private LocalDateTime createTime;

}
