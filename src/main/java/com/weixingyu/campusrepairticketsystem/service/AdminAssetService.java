package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.request.AssetRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

public interface AdminAssetService {
    Result addAsset(AssetRequest assetRequest);

    Result updateAsset(Long id, AssetRequest assetRequest);

    Result changeStatus(Long id, UpdateStatusRequest updateStatusRequest);

    Result getAssetList(Boolean enabled,Long buildingId);

    Result getAssetById(Long id);
}
