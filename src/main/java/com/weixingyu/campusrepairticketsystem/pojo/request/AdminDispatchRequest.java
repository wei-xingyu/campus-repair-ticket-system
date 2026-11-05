package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDispatchRequest {
    @NotNull(message="维修员ID不能为空") @Positive(message="维修员ID必须为正数")
    private Long assigneeId;
    @Size(max=500,message="派单说明最多500字")
    private String remark;
    @NotNull(message="版本号不能为空") @PositiveOrZero(message="版本号不能为负数")
    private Integer expectedVersion;
}
