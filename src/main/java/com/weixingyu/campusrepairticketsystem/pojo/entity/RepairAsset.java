package com.weixingyu.campusrepairticketsystem.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepairAsset {
    private Long id;
    private Long buildingId;
    private String assetCode;
    private String name;
    private String location;
    private Boolean enabled;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
