package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.request.LoginRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.LoginResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import jakarta.servlet.http.HttpSession;

public interface LoginService {
    Result login(LoginRequest loginRequest, HttpSession session);

    Result logout(HttpSession session);

    Result me(LoginResponse loginUser);
}
