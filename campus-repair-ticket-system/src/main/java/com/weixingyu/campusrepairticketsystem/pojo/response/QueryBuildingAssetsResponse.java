package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryBuildingAssetsResponse {
    private Long id;
    private String assetCode;
    private String name ;
    private String location;
    private Long buildingId;
}
