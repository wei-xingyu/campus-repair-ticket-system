package com.weixingyu.campusrepairticketsystem.pojo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentRatingRequest {
    @NotNull(message = "分数不能为空")
    @Max(value = 5,message = "评分最高不可超过5")
    @Min(value = 1,message = "评分最底不可低于1")
    private Integer score;
    @Size(max = 500,message = "评价内容，最多 500 字")
    private String comment;

}
