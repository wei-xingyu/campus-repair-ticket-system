package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.pojo.request.LoginRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.LoginService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class LoginController {
    @Resource
    private LoginService loginService;

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest loginRequest) {
        return loginService.login(loginRequest);
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String token) {
        return loginService.logout(token);
    }

    @GetMapping("/me")
    public Result me(@RequestAttribute("loginUser") LoginUser loginUser) {
        return loginService.me(loginUser);
    }
}
