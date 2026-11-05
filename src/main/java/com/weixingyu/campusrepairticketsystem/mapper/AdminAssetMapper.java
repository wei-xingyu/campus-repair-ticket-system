package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.entity.RepairAsset;
import com.weixingyu.campusrepairticketsystem.pojo.request.AssetRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminAssetMapper {
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into repair_asset(building_id, asset_code, name, location, enabled, created_at,updated_at) VALUES (#{buildingId},#{assetCode},#{name},#{location},#{enabled},now(),now())")
    void addAsset(AssetRequest assetRequest);

    @Select("select * from repair_asset r where r.id=#{id}")
    RepairAsset queryAssetById(Long id);

    @Update("update repair_asset r set r.building_id=#{assetRequest.buildingId}, r.asset_code = #{assetRequest.assetCode},r.name=#{assetRequest.name},r.location=#{assetRequest.location},r.enabled=#{assetRequest.enabled},r.updated_at=now() where id = #{id}")
    void updateAsset(@Param("id") Long id, @Param("assetRequest") AssetRequest assetRequest);

    @Update("update repair_asset r set r.enabled=#{updateStatusRequest.enabled} ,r.updated_at=now() where r.id=#{id}")
    void changeStatus(@Param("id") Long id, @Param("updateStatusRequest") UpdateStatusRequest updateStatusRequest);

    List<AssetRequest> getAssetList(@Param("enabled") Boolean enabled, @Param("buildingId") Long buildingId);

    @Select("SELECT id, building_id, name,asset_code , location , enabled FROM repair_asset WHERE id=#{id}")
    AssetRequest getAssetById(@Param("id") Long id);
}
