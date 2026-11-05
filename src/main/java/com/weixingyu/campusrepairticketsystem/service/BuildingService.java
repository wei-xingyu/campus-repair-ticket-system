package com.weixingyu.campusrepairticketsystem.service;


import com.weixingyu.campusrepairticketsystem.pojo.request.AdminBuildRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

public interface BuildingService {

    Result createBuilding(AdminBuildRequest adminBuildRequest);

    Result queryAllBuildings();

    Result queryAllBuildings(Boolean enabled);

    Result updateBuilding(Long id, AdminBuildRequest adminBuildRequest);

    Result updateBuildingStatus(Long id, UpdateStatusRequest updateStatusRequest);

    Result queryBuildingAssets(Long buildingId);

    Result getBuildingDetailed(Long id);

}
