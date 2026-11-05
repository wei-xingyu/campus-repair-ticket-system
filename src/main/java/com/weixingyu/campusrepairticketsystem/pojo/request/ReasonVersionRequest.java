package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReasonVersionRequest {
    @NotBlank(message="原因不能为空") @Size(min=2,max=500,message="原因需为2–500字")
    private String reason;
    @NotNull(message="版本号不能为空") @PositiveOrZero(message="版本号不能为负数")
    private Integer expectedVersion;
}
