package com.weixingyu.campusrepairticketsystem.service.impl;

import com.weixingyu.campusrepairticketsystem.cache.RedisCacheClient;
import com.weixingyu.campusrepairticketsystem.mapper.BuildingMapper;
import com.weixingyu.campusrepairticketsystem.pojo.request.AdminBuildRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.QueryAllBuildingResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.QueryBuildingAssetsResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.pojo.response.UpdateStatusResponse;
import com.weixingyu.campusrepairticketsystem.service.BuildingService;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class BuildingServiceImpl implements BuildingService {
    private static final String ENABLED_BUILDING_CACHE_ID = "enabled";
    @Resource
    private BuildingMapper buildingMapper;

    @Resource
    private RedisCacheClient redisCacheClient;
    @Override
    public Result createBuilding(AdminBuildRequest adminBuildRequest) {
        String key = RedisConstants.BUILDING_KEY + ENABLED_BUILDING_CACHE_ID;
        adminBuildRequest.setName(adminBuildRequest.getName().trim());
        if (adminBuildRequest.getName().length() < 2 || adminBuildRequest.getName().length() > 100) {
            return Result.error("楼宇名称长度必须在2-100字之间");
        }

        if (adminBuildRequest.getEnabled() == null) {
            adminBuildRequest.setEnabled(true);
        }
        buildingMapper.insert(adminBuildRequest);
        Long resultId = adminBuildRequest.getId();
        AdminBuildRequest result = new AdminBuildRequest(resultId, adminBuildRequest.getName(), adminBuildRequest.getAddress(), adminBuildRequest.getEnabled());
        redisCacheClient.delete(key);
        return Result.success(result);
    }

    @Override
    public Result queryAllBuildings() {
        QueryAllBuildingResponse[] data = redisCacheClient.get(
                RedisConstants.BUILDING_KEY,
                ENABLED_BUILDING_CACHE_ID,
                QueryAllBuildingResponse[].class,
                ignored -> java.util.Arrays.stream(buildingMapper.queryAllBuildings(true))
                        .map(row -> new QueryAllBuildingResponse(
                                row.getId(), row.getName(), row.getAddress()))
                        .toArray(QueryAllBuildingResponse[]::new),
                RedisConstants.BUILDING_TTL,
                TimeUnit.MINUTES
        );
        return Result.success(data);
    }

    @Override
    public Result queryAllBuildings(Boolean enabled) {
        return Result.success(buildingMapper.queryAllBuildings(enabled));
    }

    @Override
    public Result updateBuilding(Long id, AdminBuildRequest adminBuildRequest) {
        String key = RedisConstants.BUILDING_KEY + ENABLED_BUILDING_CACHE_ID;
        AdminBuildRequest old = buildingMapper.selectById(id);
        if (old == null) {
            return Result.error("楼宇不存在");
        }
        if (adminBuildRequest == null || adminBuildRequest.getName() == null) {
            adminBuildRequest.setName(old.getName());
        }
        if (adminBuildRequest.getAddress() == null) {
            adminBuildRequest.setAddress(old.getAddress());
        }
        if (adminBuildRequest.getEnabled() == null) {
            adminBuildRequest.setEnabled(old.getEnabled());
        }

        String name = adminBuildRequest.getName().trim();
        if (name.length() < 2 || name.length() > 100) {
            return Result.error("楼宇名称长度必须在2-100字之间");
        }

        adminBuildRequest.setId(id);
        adminBuildRequest.setName(name);
        buildingMapper.update(adminBuildRequest);
        redisCacheClient.delete(key);
        if (!Objects.equals(old.getEnabled(), adminBuildRequest.getEnabled())) {
            redisCacheClient.delete(RedisConstants.BUILDING_ASSET_KEY + id);
        }
        return Result.success(buildingMapper.selectById(id));
    }

    @Override
    public Result updateBuildingStatus(Long id, UpdateStatusRequest updateStatusRequest) {
        String key = RedisConstants.BUILDING_KEY +ENABLED_BUILDING_CACHE_ID;
        if (buildingMapper.selectById(id) == null) {
            return Result.error("楼宇不存在");
        }
        AdminBuildRequest old = buildingMapper.selectById(id);
        AdminBuildRequest adminBuildRequest = new AdminBuildRequest();
        adminBuildRequest.setEnabled(updateStatusRequest.getEnabled());
        adminBuildRequest.setId(id);
        adminBuildRequest.setName(old.getName());
        adminBuildRequest.setAddress(old.getAddress());
        buildingMapper.update(adminBuildRequest);
        redisCacheClient.delete(key);
        redisCacheClient.delete(RedisConstants.BUILDING_ASSET_KEY + id);
        return Result.success(new UpdateStatusResponse(id, updateStatusRequest.getEnabled()));
    }

    @Override
    public Result queryBuildingAssets(Long buildingId) {
        if (buildingMapper.selectById(buildingId) == null) {
            return Result.error("楼宇不存在");
        }
        QueryBuildingAssetsResponse[] responses=redisCacheClient.get(RedisConstants.BUILDING_ASSET_KEY,buildingId,
                QueryBuildingAssetsResponse[].class, buildingMapper::queryBuildAssets,
                RedisConstants.BUILDING_ASSET_TTL,TimeUnit.MINUTES);
        return Result.success(responses);
    }

    @Override
    public Result getBuildingDetailed(Long id) {
        AdminBuildRequest targetBuilding = buildingMapper.selectById(id);
        if (targetBuilding == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "楼宇不存在");
        }
        return Result.success(targetBuilding);
    }
}

