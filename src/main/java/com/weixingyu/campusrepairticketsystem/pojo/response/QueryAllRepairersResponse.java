package com.weixingyu.campusrepairticketsystem.pojo.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryAllRepairersResponse {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private Integer activeTicketCount;
    private List<String> specialtyCategoryNames = List.of();
    private List<String> responsibleAreaNames = List.of();

}
