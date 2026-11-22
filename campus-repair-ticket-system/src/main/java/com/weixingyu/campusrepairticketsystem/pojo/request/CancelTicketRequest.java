package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelTicketRequest {
    @Size(max = 500, message = "取消原因最多500字")
    private String reason;
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer expectedVersion;
}
