package com.weixingyu.campusrepairticketsystem.service.impl;

import com.weixingyu.campusrepairticketsystem.cache.RedisCacheClient;
import com.weixingyu.campusrepairticketsystem.mapper.AdminAssetMapper;
import com.weixingyu.campusrepairticketsystem.mapper.BuildingMapper;
import com.weixingyu.campusrepairticketsystem.pojo.entity.RepairAsset;
import com.weixingyu.campusrepairticketsystem.pojo.request.AssetRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.pojo.response.UpdateStatusResponse;
import com.weixingyu.campusrepairticketsystem.service.AdminAssetService;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAssetServiceImpl implements AdminAssetService {
    @Resource
    private AdminAssetMapper adminAssetMapper;
    @Resource
    private BuildingMapper buildingMapper;
    @Resource
    private RedisCacheClient redisCacheClient;
    @Override
    public Result addAsset(AssetRequest assetRequest) {
        String name = assetRequest.getName();

        String location = assetRequest.getLocation();

        if (buildingMapper.selectById(assetRequest.getBuildingId()) == null) {
            return Result.error("此楼宇不存在。请填入正确的楼宇");
        }
        if (assetRequest.getEnabled() == null) {
            assetRequest.setEnabled(true);
        }
        adminAssetMapper.addAsset(assetRequest);
        redisCacheClient.delete(RedisConstants.BUILDING_ASSET_KEY + assetRequest.getBuildingId());
        return Result.success(assetRequest);
    }

    @Override
    public Result updateAsset(Long id, AssetRequest assetRequest) {

        RepairAsset existing = adminAssetMapper.queryAssetById(id);
        if (existing == null) {
            return Result.error("设备不存在");
        }
        if (assetRequest.getBuildingId() == null) {
            assetRequest.setBuildingId(existing.getBuildingId());
        }
        if (assetRequest.getAssetCode() == null) {
            assetRequest.setAssetCode(existing.getAssetCode());
        }
        if (assetRequest.getName() == null) {
            assetRequest.setName(existing.getName());
        }
        if (assetRequest.getLocation() == null) {
            assetRequest.setLocation(existing.getLocation());
        }
        if (assetRequest.getEnabled() == null) {
            assetRequest.setEnabled(existing.getEnabled());
        }

        if (buildingMapper.selectById(assetRequest.getBuildingId()) == null) {
            return Result.error("此楼宇不存在。请填入正确的楼宇");
        }
        adminAssetMapper.updateAsset(id,assetRequest);
        Long oldBuildingId = existing.getBuildingId();
        Long newBuildingId = assetRequest.getBuildingId();
        redisCacheClient.delete(RedisConstants.BUILDING_ASSET_KEY + oldBuildingId);
        if (!newBuildingId.equals(oldBuildingId)) {
            redisCacheClient.delete(RedisConstants.BUILDING_ASSET_KEY + newBuildingId);
        }
        RepairAsset temp = adminAssetMapper.queryAssetById(id);
        return Result.success(new AssetRequest(temp.getId(), temp.getBuildingId(),temp.getAssetCode(), temp.getName(), temp.getLocation(),temp.getEnabled()));
    }

    @Override
    public Result changeStatus(Long id, UpdateStatusRequest updateStatusRequest) {

        RepairAsset existing = adminAssetMapper.queryAssetById(id);
        if (existing == null) {
            return Result.error("设备不存在");
        }
        RepairAsset repairAsset = adminAssetMapper.queryAssetById(id);
        if(repairAsset.getEnabled().equals(updateStatusRequest.getEnabled())){
            return Result.success(new UpdateStatusResponse(id,updateStatusRequest.getEnabled()));
        }
        adminAssetMapper.changeStatus(id,updateStatusRequest);
        redisCacheClient.delete(RedisConstants.BUILDING_ASSET_KEY + existing.getBuildingId());
        Boolean enabledResult=adminAssetMapper.queryAssetById(id).getEnabled();
        return Result.success(new UpdateStatusResponse(id,enabledResult));
    }

    @Override
    public Result getAssetList(Boolean enabled,Long buildingId) {
        return Result.success(adminAssetMapper.getAssetList(enabled,buildingId));
    }

    @Override
    public Result getAssetById(Long id) {
        AssetRequest result = adminAssetMapper.getAssetById(id);
        if(result==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在");
        }
        return Result.success(result);
    }

}
