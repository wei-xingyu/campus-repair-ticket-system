package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogRowResponse {
    private Long id;
    private Long userId;
    private String username;
    private String methodName;
    private String operation;
    private Long durationMs;
    private Integer status;
    private String targetType;
    private Long targetId;
    private String errorCode;
    private LocalDateTime createdAt;
}
