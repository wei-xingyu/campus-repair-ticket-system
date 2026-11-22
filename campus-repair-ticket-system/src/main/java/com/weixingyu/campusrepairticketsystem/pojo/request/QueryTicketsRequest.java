package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTicketsRequest {
    @Min(value=1,message="页码最小为1")
    private Integer page;
    @Min(value=1,message="每页至少1条") @Max(value=100,message="每页最多100条")
    private Integer pageSize;
    @Pattern(regexp="PENDING_ASSIGNMENT|PENDING_ACCEPTANCE|ACCEPTED|PROCESSING|PENDING_CONFIRMATION|COMPLETED|CANCELLED|CLOSED", message="工单状态无效")
    private String status;
    @Positive(message="categoryId必须为正数")
    private Long categoryId;
    @Positive(message="buildingId必须为正数")
    private Long buildingId;
    @Pattern(regexp="LOW|NORMAL|HIGH|URGENT", message="优先级无效")
    private String priority;
    @Size(max=100,message="关键词最多100字")
    private String keyword;
    @jakarta.validation.constraints.Size(max = 100)
    private String reporterKeyword;
    @jakarta.validation.constraints.Size(max = 100)
    private String assigneeKeyword;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdTo;
    @Positive(message="reporterId必须为正数")
    private Long reporterId;
    @Positive(message="assigneeId必须为正数")
    private Long assigneeId;
}
