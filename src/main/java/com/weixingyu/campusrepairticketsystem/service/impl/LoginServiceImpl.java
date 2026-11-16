package com.weixingyu.campusrepairticketsystem.service.impl;

import cn.hutool.core.lang.UUID;
import com.weixingyu.campusrepairticketsystem.cache.LoginSessionStore;
import com.weixingyu.campusrepairticketsystem.mapper.LoginMapper;
import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.pojo.entity.SysUser;
import com.weixingyu.campusrepairticketsystem.pojo.request.LoginRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.LoginResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.LoginService;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Resource
    LoginMapper loginMapper;

    @Resource
    private LoginSessionStore loginSessionStore;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Result login(LoginRequest loginRequest) {

        SysUser sysUser = loginMapper.findByUsername(loginRequest.getUsername());
        if (sysUser == null || !passwordEncoder.matches(
                loginRequest.getPassword(), sysUser.getPasswordHash())) {
            return Result.error("用户账号或密码错误");
        }

        if (sysUser.getEnabled() == null || sysUser.getEnabled() != 1) {
            return Result.error("账号已停用");
        }

        String roleCode = loginMapper.findRoleCodeByUserId(sysUser.getId());
        if (roleCode == null) {
            return Result.error("账号未分配角色");
        }

        LoginResponse loginResponse = new LoginResponse();
        loginResponse.setId(sysUser.getId());
        loginResponse.setName(sysUser.getName());
        loginResponse.setUsername(sysUser.getUsername());
        loginResponse.setRole(roleCode);
        String token = UUID.randomUUID().toString();
        String key = RedisConstants.LOGIN_USER_KEY + token;
        loginResponse.setToken(token);
        LoginUser loginUser = new LoginUser();
        loginUser.setId(sysUser.getId());
        loginUser.setName(sysUser.getName());
        loginUser.setRole(roleCode);
        loginUser.setUsername(sysUser.getUsername());
        loginSessionStore.save(token,loginUser);
        return Result.success(loginResponse);
    }

    @Override
    public Result logout( LoginUser user,String token) {
        loginSessionStore.revokeOne(token, user.getId());
        log.info("用户成功退出登录");
        return Result.success();
    }

    @Override
    public Result me(LoginUser loginUser) {
        return Result.success(loginUser);
    }
}
