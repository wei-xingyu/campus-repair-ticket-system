package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketHistoryResponse {
    private Long id;
    private String fromStatus;
    private String toStatus;
    private Long operatorId;
    private String operatorName;
    private String action;
    private String remark;
    private LocalDateTime createdAt;
}
