package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpectedVersionRequest {
    @NotNull(message="版本号不能为空") @PositiveOrZero(message="版本号不能为负数")
    private Integer expectedVersion;
}
