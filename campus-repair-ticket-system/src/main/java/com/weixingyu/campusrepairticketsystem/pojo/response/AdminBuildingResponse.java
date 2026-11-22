package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.Data;

@Data
public class AdminBuildingResponse {
    private Long id;
    private String name;
    private String address;
    private Boolean enabled;
}
