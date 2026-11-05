package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetRequest {
    private Long id;
    @NotNull(message="楼宇ID不能为空") @Positive(message="楼宇ID必须为正数")
    private Long buildingId;
    @Size(max=64,message="设备编码最多64字")
    private String assetCode;//选填
    @NotBlank(message="设备名称不能为空") @Size(max=100,message="设备名称最多100字")
    private String name;//1-100字
    @Size(max=100,message="设备位置最多100字")
    private String location;//选填，最多100字
    private Boolean enabled;


}
