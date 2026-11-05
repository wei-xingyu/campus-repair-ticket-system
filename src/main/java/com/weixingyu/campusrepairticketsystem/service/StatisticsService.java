package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.request.StatisticsRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

public interface StatisticsService {
    Result queryTicketStatistics(StatisticsRequest request);

}
