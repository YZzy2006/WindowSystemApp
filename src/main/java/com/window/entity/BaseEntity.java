package com.window.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

@Data
public class BaseEntity {
    @TableLogic
    private Integer deleted = 0;
}
