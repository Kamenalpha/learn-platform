package com.rag.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    private String username;

    private String password;

    private String nickname;

    /** 头像URL */
    private String avatar;

    private String email;

    private String phone;

    /** 角色:0用户(学习者) 1管理员 */
    private Integer role;

    /** 状态:1启用 0禁用 */
    private Integer status;

    private LocalDateTime createTime;
}
