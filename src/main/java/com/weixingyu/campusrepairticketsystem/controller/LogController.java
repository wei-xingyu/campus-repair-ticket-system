package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.LogService;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin")
public class LogController {

    @Resource
    private LogService logService;

    @RequireRole("ADMIN")
    @GetMapping("/operation-logs")
    public Result getLogs(@RequestParam(name = "userId", required = false) Long userId,
                          @RequestParam(name = "status", required = false) Integer status,
                          @RequestParam(name = "startTime", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                          @RequestParam(name = "endTime", required = false)
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                          @RequestParam(name = "page", required = false) Integer page,
                          @RequestParam(name = "pageSize", required = false) Integer pageSize) {
        return logService.getLogs(userId,status,startTime,endTime,page,pageSize);
    }
}
