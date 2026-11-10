package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.pojo.request.LoginRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

public interface LoginService {
    Result login(LoginRequest loginRequest);

    Result logout(String token);

    Result me(LoginUser loginUser);
}
