package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepairManageResponse {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private Integer activeTicketCount;
    private Boolean enabled;
}
