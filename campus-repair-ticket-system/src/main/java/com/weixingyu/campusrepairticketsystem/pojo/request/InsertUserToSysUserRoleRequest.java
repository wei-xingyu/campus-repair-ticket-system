package com.weixingyu.campusrepairticketsystem.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InsertUserToSysUserRoleRequest {
    private Long userId;
    private Long roleId;
    private LocalDateTime createdAt;
}
