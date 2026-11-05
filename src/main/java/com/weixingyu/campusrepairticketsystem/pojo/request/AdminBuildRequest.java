package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminBuildRequest {
    private Long id;
    @NotBlank(message="楼宇名称不能为空") @Size(min=2,max=100,message="楼宇名称需为2–100字")
    private String name;//必填，2-100字
    @Size(max=200,message="楼宇地址最多200字")
    private String address;//选填，0-200字
    private Boolean enabled;//选填，默认true
}
