package com.rag.edu.controller;

import com.rag.edu.common.LoginUser;
import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.dto.AuthDtos.LoginReq;
import com.rag.edu.dto.AuthDtos.LoginResp;
import com.rag.edu.dto.AuthDtos.RegisterReq;
import com.rag.edu.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterReq req) {
        authService.register(req);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(UserContext.get());
    }
}
