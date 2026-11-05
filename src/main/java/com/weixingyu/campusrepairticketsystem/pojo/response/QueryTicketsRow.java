package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTicketsRow {
    private Long id;
    private String ticketNo;
    private String title;
    private String categoryName;
    private String buildingName;
    private String room;
    private String priority;
    private String status;
    private String reporterName;
    private String assigneeName;
    private String createdAt;
    private String updatedAt;
}
