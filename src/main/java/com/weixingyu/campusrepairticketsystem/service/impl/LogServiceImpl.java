package com.weixingyu.campusrepairticketsystem.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.weixingyu.campusrepairticketsystem.mapper.LogMapper;
import com.weixingyu.campusrepairticketsystem.pojo.response.*;
import com.weixingyu.campusrepairticketsystem.service.LogService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogServiceImpl implements LogService {

    @Resource
    private LogMapper logMapper;

    @Override
    public Result getLogs(Long userId, Integer status, LocalDateTime startTime, LocalDateTime endTime, Integer page, Integer pageSize) {
        int currentPage = page == null ? 1 : page;
        int currentPageSize = pageSize == null ? 10 : pageSize;
        if (userId != null && userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户 ID 必须为正整数");
        }
        if (status != null && status != 0 && status != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "执行结果只能为 0 或 1");
        }
        if (currentPage < 1 || currentPageSize < 1 || currentPageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "页码必须大于等于 1，每页条数必须在 1 至 100 之间");
        }
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开始时间必须早于结束时间");
        }

        PageHelper.startPage(currentPage, currentPageSize);
        List<LogRowResponse> rows = logMapper.queryByPage(userId, status, startTime, endTime);
        PageInfo<LogRowResponse> pageInfo = new PageInfo<>(rows);
        return Result.success(new LogResponse(pageInfo.getTotal(), rows, currentPage, currentPageSize));
    }
}
