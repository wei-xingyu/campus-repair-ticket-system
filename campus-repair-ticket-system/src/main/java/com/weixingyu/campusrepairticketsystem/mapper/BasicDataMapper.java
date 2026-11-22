package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.request.AdminCategoryRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.CategoriesResponse;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BasicDataMapper {
    @Select("select id, name, description, sort_order from campus_repair.repair_category where enabled = 1 order by sort_order")
    CategoriesResponse[] queryCategories();

    @Select("""
            <script>
            select id, name, description, sort_order, enabled from campus_repair.repair_category
            <where>
                <if test="enabled != null">enabled = #{enabled}</if>
            </where>
            order by sort_order, id
            </script>
            """)
    java.util.List<AdminCategoryRequest> queryAdminCategories(@Param("enabled") Boolean enabled);

    @Select("select id, name, description, sort_order, enabled from campus_repair.repair_category where id = #{id}")
    AdminCategoryRequest findCategoryById(@Param("id") Long id);

    @Select("select count(*) from campus_repair.repair_category where name = #{name}")
    int countCategoryName(@Param("name") String name);

    @Select("select count(*) from campus_repair.repair_category where name = #{name} and id <> #{excludeId}")
    int countCategoryNameExceptId(@Param("name") String name, @Param("excludeId") Long excludeId);

    @Insert("insert into campus_repair.repair_category (name, description, sort_order, enabled) values (#{name}, #{description}, #{sortOrder}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int createCategory(AdminCategoryRequest adminCategoryRequest);

    @Update("update campus_repair.repair_category set name = #{name}, description = #{description}, sort_order = #{sortOrder}, enabled = #{enabled} where id = #{id}")
    int updateCategory(AdminCategoryRequest adminCategoryRequest);

    @Update("update campus_repair.repair_category set enabled = #{enabled} where id = #{id}")
    int updateCategoryStatus(@Param("id") Long id, @Param("enabled") boolean enabled);
}
