package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginMapper {
    @Select("""
        SELECT id, username, password_hash, name, phone, enabled,
               created_at, updated_at
        FROM sys_user
        WHERE username = #{username}
        """)
    SysUser findByUsername(@Param("username") String username);

    @Select("""
        SELECT r.role_code
        FROM sys_user_role ur
        JOIN sys_role r ON r.id = ur.role_id
        WHERE ur.user_id = #{userId}
        """)
    String findRoleCodeByUserId(@Param("userId") Long userId);
}
