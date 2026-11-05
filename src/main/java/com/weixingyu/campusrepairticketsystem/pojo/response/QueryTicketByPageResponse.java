package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryTicketByPageResponse {
    private Long total;
    private List<QueryTicketsRow> rows;
    private Integer page;
    private Integer pageSize;

}
