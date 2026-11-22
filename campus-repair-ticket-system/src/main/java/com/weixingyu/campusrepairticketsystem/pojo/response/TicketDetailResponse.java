package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketDetailResponse {
    private Long id;
    private String ticketNo;
    private String title;
    private String description;
    private Long categoryId;
    private Long buildingId;
    private Long assetId;
    private String categoryName;
    private String buildingName;
    private String assetName;
    private String room;
    private String priority;
    private String status;
    private String reporterName;
    private String assigneeName;
    private List<TicketAttachmentResponse> attachments;
    private String resolution;
    private Integer version;
    private LocalDateTime slaDueAt;
    private List<TicketHistoryResponse> history;
    private java.time.LocalDateTime createdAt;
}
