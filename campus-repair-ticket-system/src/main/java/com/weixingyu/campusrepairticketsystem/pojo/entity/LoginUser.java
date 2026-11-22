package com.weixingyu.campusrepairticketsystem.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser {

    private Long id;
    private String username;
    private String name;
    private String role;
}

