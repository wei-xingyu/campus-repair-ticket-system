package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsResponse {
    private Integer totalCount;
    private Integer pendingAssignmentCount;
    private Integer processingCount;
    private Integer pendingConfirmationCount;
    private Integer completedCount;
    private Integer overdueCount;
    private BigDecimal averageCompletionMinutes;
    private List<CategoryCountResponse> categoryCounts;
}
