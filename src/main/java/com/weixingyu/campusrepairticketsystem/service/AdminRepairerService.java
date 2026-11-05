package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.request.CreateRepairerRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

public interface AdminRepairerService {
    Result queryAllRepairers(String keyword);

    Result createRepairer(CreateRepairerRequest request);

    Result updateRepairerStatus(Long id, UpdateStatusRequest updateStatusRequest);

    Result getRepairList(Boolean enabled, String keyword);

    Result getRepairerDetailed(Long id);
}
