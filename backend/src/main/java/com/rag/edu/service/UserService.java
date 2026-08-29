package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.entity.SysUser;
import com.rag.edu.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户管理(管理员)
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;

    public List<SysUser> list() {
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getUserId));
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    public void updateRole(Long userId, Integer role) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setRole(role);
        userMapper.updateById(user);
    }

    public void delete(Long userId) {
        if (userId.equals(com.rag.edu.common.UserContext.userId())) {
            throw new BizException("不能删除当前登录账号");
        }
        userMapper.deleteById(userId);
    }
}
