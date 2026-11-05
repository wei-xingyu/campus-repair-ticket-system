package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.pojo.request.LoginRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.LoginResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.LoginService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
public class LoginController {
    @Resource
    private LoginService loginService;

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest loginRequest, HttpSession session) {
        return loginService.login(loginRequest, session);
    }

    @PostMapping("/logout")
    public Result logout(HttpSession session) {
        return loginService.logout(session);
    }

    @GetMapping("/me")
    public Result me(@RequestAttribute("loginUser") LoginResponse loginUser) {
        return loginService.me(loginUser);
    }
}
