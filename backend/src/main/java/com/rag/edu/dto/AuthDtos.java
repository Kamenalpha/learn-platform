package com.rag.edu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 认证模块 DTO
 */
public class AuthDtos {

    public record LoginReq(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record RegisterReq(
            @NotBlank(message = "用户名不能为空") @Size(min = 3, max = 20, message = "用户名长度3-20位") String username,
            @NotBlank(message = "密码不能为空") @Size(min = 6, max = 32, message = "密码长度6-32位") String password,
            String nickname) {
    }

    public record LoginResp(String token, Long userId, String username, String nickname, Integer role) {
    }
}
