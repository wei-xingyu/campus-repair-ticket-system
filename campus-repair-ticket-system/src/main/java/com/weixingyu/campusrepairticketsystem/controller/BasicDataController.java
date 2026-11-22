package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.request.AdminCategoryRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.service.BasicDataService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
public class BasicDataController {
    @Resource
    private BasicDataService basicDataService;

    @RequireRole({"ADMIN", "REPAIRER", "STUDENT"})
    @GetMapping("/categories")
    public Result queryCategories() {
        return basicDataService.queryCategories();
    }

    @RequireRole("ADMIN")
    @GetMapping("/admin/categories")
    public Result queryAdminCategories(@RequestParam(name = "enabled", required = false) Boolean enabled) {
        return basicDataService.queryAdminCategories(enabled);
    }

    @RequireRole("ADMIN")
    @PostMapping("/admin/categories")
    public Result createCategory(@Valid @RequestBody AdminCategoryRequest adminCategoryRequest) {
        return basicDataService.createCategory(adminCategoryRequest);
    }

    @RequireRole("ADMIN")
    @PutMapping("/admin/categories/{id}")
    public Result updateCategory(@PathVariable @Positive(message="ID必须为正数") Long id,
                                 @Valid @RequestBody AdminCategoryRequest adminCategoryRequest) {
        return basicDataService.updateCategory(id, adminCategoryRequest);
    }

    @RequireRole("ADMIN")
    @PutMapping("/admin/categories/{id}/status")
    public Result updateCategoryStatus(@PathVariable @Positive(message="ID必须为正数") Long id,
                                      @Valid @RequestBody UpdateStatusRequest request) {
        AdminCategoryRequest status = new AdminCategoryRequest();
        status.setEnabled(request.getEnabled());
        return basicDataService.updateCategoryStatus(id, status);
    }
}
