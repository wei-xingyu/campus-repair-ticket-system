package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.request.AdminCategoryRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;

public interface BasicDataService {
    Result queryCategories();

    Result queryAdminCategories(Boolean enabled);

    Result createCategory(AdminCategoryRequest adminCategoryRequest);

    Result updateCategory(Long id, AdminCategoryRequest adminCategoryRequest);

    Result updateCategoryStatus(Long id, AdminCategoryRequest adminCategoryRequest);
}
