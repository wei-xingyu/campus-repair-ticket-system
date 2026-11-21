package com.weixingyu.campusrepairticketsystem.pojo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogResponse {
    private Long total;
    private List<LogRowResponse> rows;
    private Integer page;
    private Integer pageSize;
}
