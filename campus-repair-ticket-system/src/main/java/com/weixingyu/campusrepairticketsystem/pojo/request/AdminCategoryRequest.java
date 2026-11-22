package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCategoryRequest {
    private Long id;
    @NotBlank(message="类别名称不能为空") @Size(min=2,max=50,message="类别名称需为2–50字")
    private String name;
    @Size(max=200,message="类别说明最多200字")
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
}
