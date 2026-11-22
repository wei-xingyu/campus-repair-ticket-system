package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

import java.time.LocalDateTime;

public interface LogService {
    Result getLogs(Long userId, Integer status, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer pageSize);

}
