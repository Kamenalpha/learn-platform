package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.entity.SysUser;
import com.rag.edu.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理(管理员)
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<List<SysUser>> list() {
        return Result.ok(userService.list());
    }

    @PutMapping("/{userId}/role")
    public Result<Void> updateRole(@PathVariable Long userId, @RequestParam Integer role) {
        userService.updateRole(userId, role);
        return Result.ok();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Long userId) {
        userService.delete(userId);
        return Result.ok();
    }
}
