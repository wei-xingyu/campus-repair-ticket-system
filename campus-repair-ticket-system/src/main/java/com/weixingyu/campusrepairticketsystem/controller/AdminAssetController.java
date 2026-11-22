package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.request.AssetRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.AdminAssetService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/assets")
public class AdminAssetController {
    @Resource
    private AdminAssetService adminAssetService;


    @RequireRole("ADMIN")
    @PostMapping()
    public Result addAsset(@Valid @RequestBody AssetRequest assetRequest){
        return adminAssetService.addAsset(assetRequest);
    }

    @RequireRole("ADMIN")
    @PutMapping("/{id}")
    public Result updateAsset(@PathVariable @Positive(message="ID必须为正数") Long id, @Valid @RequestBody AssetRequest assetRequest){
        return adminAssetService.updateAsset(id,assetRequest);
    }

    @RequireRole("ADMIN")
    @PutMapping({"/{id}/status"})
    public Result changeStatus(@PathVariable @Positive(message="ID必须为正数") Long id ,@Valid @RequestBody UpdateStatusRequest updateStatusRequest){
        return adminAssetService.changeStatus(id,updateStatusRequest);
    }

    @RequireRole("ADMIN")
    @GetMapping()
    public Result getAssetList(@RequestParam(name = "enabled", required = false)Boolean enabled,
                               @RequestParam(name = "buildingId", required = false) @Positive(message = "ID必须为正数") Long buildingId){
        return adminAssetService.getAssetList(enabled,buildingId);
    }

    @RequireRole("ADMIN")
    @GetMapping("/{id}")
    public Result getAssetById(@PathVariable @Positive(message = "ID必须为正数") Long id){
        return adminAssetService.getAssetById(id);
    }
}
