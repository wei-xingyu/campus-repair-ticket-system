package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.request.CreateRepairerRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.AdminRepairerService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminRepairerController {
    @Resource
    private AdminRepairerService adminRepairerService;

    @RequireRole("ADMIN")
    @GetMapping("/repairers")
    public Result queryAllRepairers(@RequestParam(required = false, defaultValue = "") @Size(max=100,message="关键词最多100字") String keyword){
        return adminRepairerService.queryAllRepairers(keyword);
    }

    @RequireRole("ADMIN")
    @PostMapping("/repairers")
    public Result createRepairer(@Valid @RequestBody CreateRepairerRequest request){
        return adminRepairerService.createRepairer(request);
    }

    @PutMapping("/repairers/{id}/status")
    @RequireRole("ADMIN")
    public Result updateRepairerStatus(@PathVariable @Positive(message="ID必须为正数") Long id, @Valid @RequestBody UpdateStatusRequest updateStatusRequest) {
        return adminRepairerService.updateRepairerStatus(id, updateStatusRequest);
    }

    @RequireRole("ADMIN")
    @GetMapping("/repairers/manage")
    public Result getRepairersList(@RequestParam(name = "enabled", required = false)Boolean enabled,
                                   @RequestParam(name = "keyword", required = false)String keyword){
        return adminRepairerService.getRepairList(enabled,keyword);
    }

    @RequireRole("ADMIN")
    @GetMapping("/admin/repairers/{id}")
    public Result getRepairerDetailed(@PathVariable @Positive(message="ID必须为正数") Long id){
        return adminRepairerService.getRepairerDetailed(id);
    }
}
