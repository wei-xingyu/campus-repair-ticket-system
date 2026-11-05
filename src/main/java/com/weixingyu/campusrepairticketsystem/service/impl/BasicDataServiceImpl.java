package com.weixingyu.campusrepairticketsystem.service.impl;

import com.weixingyu.campusrepairticketsystem.mapper.BasicDataMapper;
import com.weixingyu.campusrepairticketsystem.pojo.request.AdminCategoryRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.BasicDataService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BasicDataServiceImpl implements BasicDataService {
    @Resource
    private BasicDataMapper basicDataMapper;

    @Override
    public Result queryCategories() {
        return Result.success(basicDataMapper.queryCategories());
    }

    @Override
    public Result queryAdminCategories(Boolean enabled) {
        return Result.success(basicDataMapper.queryAdminCategories(enabled));
    }

    @Override
    public Result createCategory(AdminCategoryRequest adminCategoryRequest) {

        String name = adminCategoryRequest.getName().trim();
        if (name.length() < 2 || name.length() > 50) {
            return Result.error("类别名称长度必须为2到50个字符");
        }

        adminCategoryRequest.setName(name);
        if (adminCategoryRequest.getSortOrder() == null) {
            adminCategoryRequest.setSortOrder(0);
        }
        if (adminCategoryRequest.getEnabled() == null) {
            adminCategoryRequest.setEnabled(true);
        }
        if (basicDataMapper.countCategoryName(adminCategoryRequest.getName()) > 0) {
            return Result.error("故障类别名称已存在");
        }

        basicDataMapper.createCategory(adminCategoryRequest);
        return Result.success(adminCategoryRequest);
    }

    @Override
    public Result updateCategory(Long id, AdminCategoryRequest adminCategoryRequest) {

        String name = adminCategoryRequest.getName().trim();
        if (name.length() < 2 || name.length() > 50) {
            return Result.error("类别名称长度必须为2到50个字符");
        }

        AdminCategoryRequest existing = basicDataMapper.findCategoryById(id);
        if (existing == null) {
            return Result.error("故障类别不存在");
        }

        adminCategoryRequest.setName(name);
        if (basicDataMapper.countCategoryNameExceptId(adminCategoryRequest.getName(), id) > 0) {
            return Result.error("故障类别名称已存在");
        }

        adminCategoryRequest.setId(id);
        if (adminCategoryRequest.getDescription() == null) {
            adminCategoryRequest.setDescription(existing.getDescription());
        }
        if (adminCategoryRequest.getSortOrder() == null) {
            adminCategoryRequest.setSortOrder(existing.getSortOrder());
        }
        if (adminCategoryRequest.getEnabled() == null) {
            adminCategoryRequest.setEnabled(existing.getEnabled());
        }

        basicDataMapper.updateCategory(adminCategoryRequest);
        return Result.success(adminCategoryRequest);
    }

    @Override
    public Result updateCategoryStatus(Long id, AdminCategoryRequest adminCategoryRequest) {

        AdminCategoryRequest existing = basicDataMapper.findCategoryById(id);
        if (existing == null) {
            return Result.error("故障类别不存在");
        }

        basicDataMapper.updateCategoryStatus(id, adminCategoryRequest.getEnabled());
        return Result.success(Map.of("id", id, "enabled", adminCategoryRequest.getEnabled()));
    }
}
