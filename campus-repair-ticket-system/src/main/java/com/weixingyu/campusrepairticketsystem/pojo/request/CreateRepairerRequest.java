package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRepairerRequest {
    @NotBlank(message="用户名不能为空") @Size(min=2,max=64,message="用户名需为2–64字")
    private String username;
    @NotBlank(message="密码不能为空") @Size(min=8,max=72,message="密码需为8–72字")
    private String password;
    @NotBlank(message="姓名不能为空") @Size(min=1,max=64,message="姓名需为1–64字")
    private String name;
    @Size(max=20,message="手机号最多20字")
    private String phone;
}
