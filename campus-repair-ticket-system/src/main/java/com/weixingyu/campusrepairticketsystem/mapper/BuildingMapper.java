package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.request.AdminBuildRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.AdminBuildingResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import com.weixingyu.campusrepairticketsystem.pojo.response.QueryBuildingAssetsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BuildingMapper {
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into building (name, address, enabled) values (#{name}, #{address}, #{enabled})")
    void insert(AdminBuildRequest adminBuildRequest);

    @Select("select id from building where name=#{name} and enabled=#{enabled} and address=#{address}")
    Long selectId(AdminBuildRequest adminBuildRequest);

    @Select("select id, name, address, enabled from building where id=#{id}")
    AdminBuildRequest selectById(Long id);

    AdminBuildingResponse[] queryAllBuildings(@Param("enabled") Boolean enabled);

    @Update("update building set name=#{name}, address=#{address}, enabled=#{enabled} where id=#{id}")
    void update(AdminBuildRequest adminBuildRequest);

    @Select("select r.id, r.asset_code, r.name, r.location, r.building_id from building b join repair_asset r on b.id = r.building_id where b.id = #{buildingId} and b.enabled = 1 and r.enabled = 1 order by r.id")
    QueryBuildingAssetsResponse[] queryBuildAssets(Long buildingId);
}
