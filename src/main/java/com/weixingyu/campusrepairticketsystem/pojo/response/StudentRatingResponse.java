package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentRatingResponse {
    private Long id;
    private Long ticketId;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
