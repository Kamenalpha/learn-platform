package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.common.JwtUtil;
import com.rag.edu.common.LoginUser;
import com.rag.edu.dto.AuthDtos.LoginReq;
import com.rag.edu.dto.AuthDtos.LoginResp;
import com.rag.edu.dto.AuthDtos.RegisterReq;
import com.rag.edu.entity.SysUser;
import com.rag.edu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 认证服务:注册 / 登录
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterReq req) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username()));
        if (exists > 0) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname());
        user.setRole(0); // 自主注册默认学生角色
        userMapper.insert(user);
    }

    public LoginResp login(LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.username()));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        LoginUser lu = new LoginUser(user.getUserId(), user.getUsername(), user.getRole());
        return new LoginResp(jwtUtil.createToken(lu), user.getUserId(), user.getUsername(),
                user.getNickname(), user.getRole());
    }
}
