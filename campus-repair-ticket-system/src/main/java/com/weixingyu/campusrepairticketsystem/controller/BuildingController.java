package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.request.AdminBuildRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.BuildingService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
public class BuildingController {
    @Resource
    private BuildingService buildingService;

    //查询楼宇
    @GetMapping("/buildings")
    @RequireRole({"ADMIN", "STUDENT", "REPAIRER"})
    public Result queryAllBuildings() {
        return buildingService.queryAllBuildings();
    }

    //新增楼宇
    @PostMapping("/admin/buildings")
    @RequireRole("ADMIN")
    public Result createBuilding(@Valid @RequestBody AdminBuildRequest adminBuildRequest) {
        return buildingService.createBuilding(adminBuildRequest);
    }

    //修改指定楼宇信息
    @PutMapping("/admin/buildings/{id}")
    @RequireRole("ADMIN")
    public Result updateBuilding(@PathVariable @Positive(message="ID必须为正数") Long id, @Valid @RequestBody AdminBuildRequest adminBuildRequest) {
        return buildingService.updateBuilding(id, adminBuildRequest);
    }

    //停用指定楼宇
    @PutMapping("/admin/buildings/{id}/status")
    @RequireRole("ADMIN")
    public Result updateBuildingStatus(@PathVariable @Positive(message="ID必须为正数") Long id, @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {
        return buildingService.updateBuildingStatus(id, updateStatusRequest);
    }

    @RequireRole({"ADMIN","STUDENT","REPAIRER"})
    @GetMapping("/buildings/{buildingId}/assets")
    public Result queryBuildingAssets(@PathVariable @Positive(message="ID必须为正数") Long buildingId){
        return buildingService.queryBuildingAssets(buildingId);
    }

    @RequireRole("ADMIN")
    @GetMapping("/admin/buildings")
    public Result queryBuildings(@RequestParam(name = "enabled", required = false) Boolean enabled){
        return buildingService.queryAllBuildings(enabled);
    }

    @RequireRole("ADMIN")
    @GetMapping("/admin/buildings/{id}")
    public Result getBuildingDetailed(@PathVariable @Positive(message = "ID必须为正数") Long id){
        return buildingService.getBuildingDetailed(id);
    }
}
