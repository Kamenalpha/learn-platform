package com.rag.edu.config;

import com.rag.edu.entity.SysUser;
import com.rag.edu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 初始数据:首次启动时若用户表为空,自动创建管理员与学生账号。
 * 管理员 admin / admin123,学生 student / 123456
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setNickname("管理员");
        admin.setRole(1);

        SysUser student = new SysUser();
        student.setUsername("student");
        student.setPassword(passwordEncoder.encode("123456"));
        student.setNickname("测试学生");
        student.setRole(0);

        userMapper.insert(admin);
        userMapper.insert(student);
        log.info("初始用户已创建: admin/admin123(管理员), student/123456(学生)");
    }
}
