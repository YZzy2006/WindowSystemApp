package com.window.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("login_log")
public class LoginLog {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer adminId;
    private String username;
    private String loginIp;
    private String userAgent;
    private Integer status = 1;   // 1成功 0失败
    private String failReason;
    private String remark;
    private LocalDateTime loginTime;
}
