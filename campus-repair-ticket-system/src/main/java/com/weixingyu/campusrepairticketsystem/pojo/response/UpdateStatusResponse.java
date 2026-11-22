package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateStatusResponse {
    private Long id;
    private Boolean enabled;
}
