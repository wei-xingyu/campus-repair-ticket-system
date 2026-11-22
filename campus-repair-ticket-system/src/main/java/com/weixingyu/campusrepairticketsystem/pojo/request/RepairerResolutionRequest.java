package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepairerResolutionRequest {
    @NotBlank(message = "处理说明不能为空")
    @Size(min = 5, max = 2000, message = "处理说明需为5–2000字")
    private String resolution;
    @Size(max = 5, message = "处理图片最多5张")
    private String[] attachmentUrls;
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer expectedVersion;
}
