package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.entity.SysUser;
import com.weixingyu.campusrepairticketsystem.pojo.request.InsertUserToSysUserRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.InsertUserToSysUserRoleRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.QueryAllRepairersResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.RepairManageResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.UpdateStatusResponse;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminRepairerMapper {

    QueryAllRepairersResponse[] queryAllRepairers(String keyword);

    @Insert("INSERT INTO sys_user(username, password_hash, name, phone , enabled, created_at, updated_at) VALUES(#{username}, #{passwordHash}, #{name}, #{phone}, #{enabled}, #{createdAt}, #{updatedAt} )")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertIntoSysUser(InsertUserToSysUserRequest insertUserToSysUserRequest);

    @Insert("INSERT INTO sys_user_role(user_id, role_id, created_at) VALUES(#{userId}, #{roleId}, #{createdAt})")
    void insertIntoSysUserRole(InsertUserToSysUserRoleRequest insertUserToSysUserRoleRequest);

    @Select("SELECT u.* FROM sys_user u JOIN sys_user_role ur ON u.id = ur.user_id JOIN sys_role r ON r.id = ur.role_id WHERE r.role_code = 'REPAIRER' AND u.id = #{id}")
    SysUser selectRepairerById(Long id);

    @Select("SELECT u.id, u.username, u.name, u.phone, u.enabled " +
            "FROM sys_user u " +
            "JOIN sys_user_role ur ON ur.user_id = u.id " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE u.id = #{id} AND u.enabled = 1 AND r.role_code = 'REPAIRER'")
    SysUser findEnabledRepairerById(@Param("id") Long id);

    @Update("UPDATE sys_user SET enabled = #{enabled} WHERE id = #{id}")
    void updateEnabled(@Param("id") Long id, @Param("enabled") Boolean enabled);

    @Select("SELECT id FROM sys_role WHERE role_code = 'REPAIRER'")
    Long findRepairerRoleId();

    @Select("SELECT id,enabled FROM sys_user WHERE id = #{id}")
    UpdateStatusResponse getUpdateStatus(Long id);

    List<RepairManageResponse> getRepairList(Boolean enabled,String keyword);

    RepairManageResponse findRepairerById(Long id);
}
