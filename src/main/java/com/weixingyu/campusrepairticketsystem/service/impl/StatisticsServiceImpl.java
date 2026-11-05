package com.weixingyu.campusrepairticketsystem.service.impl;

import com.weixingyu.campusrepairticketsystem.mapper.StatisticsMapper;
import com.weixingyu.campusrepairticketsystem.pojo.request.StatisticsRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.pojo.response.StatisticsResponse;
import com.weixingyu.campusrepairticketsystem.service.StatisticsService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.math.RoundingMode;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    @Resource
    private StatisticsMapper statisticsMapper;

    @Override
    public Result queryTicketStatistics(StatisticsRequest request) {

        if (!request.getTo().isAfter(request.getFrom())) {
            return Result.error("to 必须晚于 from");
        }
        if (Duration.between(request.getFrom(), request.getTo()).compareTo(Duration.ofDays(366)) > 0) {
            return Result.error("查询时间范围不能超过366天");
        }

        StatisticsResponse response = statisticsMapper.querySummary(request);
        if (response.getAverageCompletionMinutes() == null) {
            response.setAverageCompletionMinutes(java.math.BigDecimal.ZERO.setScale(1));
        } else {
            response.setAverageCompletionMinutes(response.getAverageCompletionMinutes()
                    .setScale(1, RoundingMode.HALF_UP));
        }
        response.setCategoryCounts(statisticsMapper.categoryCounts(request));
        return Result.success(response);
    }
}
