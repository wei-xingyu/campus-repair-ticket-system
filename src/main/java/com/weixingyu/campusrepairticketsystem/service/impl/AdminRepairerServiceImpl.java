package com.weixingyu.campusrepairticketsystem.service.impl;

import com.weixingyu.campusrepairticketsystem.cache.LoginSessionStore;
import com.weixingyu.campusrepairticketsystem.mapper.AdminRepairerMapper;
import com.weixingyu.campusrepairticketsystem.pojo.request.CreateRepairerRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.InsertUserToSysUserRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.InsertUserToSysUserRoleRequest;
import com.weixingyu.campusrepairticketsystem.pojo.request.UpdateStatusRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.*;
import com.weixingyu.campusrepairticketsystem.service.AdminRepairerService;
import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminRepairerServiceImpl implements AdminRepairerService {
    @Resource
    private AdminRepairerMapper adminRepairerMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private LoginSessionStore loginSessionStore;

    @Override
    public Result queryAllRepairers(String keyword) {

        QueryAllRepairersResponse[] queryAllRepairersResponses = adminRepairerMapper.queryAllRepairers(keyword);
        return Result.success(queryAllRepairersResponses);
    }

    @Transactional
    @Override
    public Result createRepairer(CreateRepairerRequest request) {

        request.setName(request.getName().trim());
        request.setUsername(request.getUsername().trim());

        if (request.getPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) return Result.error("密码UTF-8编码后不能超过72字节");
        if (request.getName().length() > 64 || request.getName().length() < 1) {
            return Result.error("姓名长度必须在1到64之间");
        }

        if (request.getUsername().length() > 64 || request.getUsername().length() < 2) {
            return Result.error("用户名长度必须在2到64之间");
        }
        String passwordHash = passwordEncoder.encode(request.getPassword());
        InsertUserToSysUserRequest insertUserToSysUserRequest = new InsertUserToSysUserRequest();
        insertUserToSysUserRequest.setUsername(request.getUsername());
        insertUserToSysUserRequest.setPasswordHash(passwordHash);
        insertUserToSysUserRequest.setName(request.getName());
        insertUserToSysUserRequest.setPhone(request.getPhone());
        insertUserToSysUserRequest.setEnabled(true);
        insertUserToSysUserRequest.setCreatedAt(LocalDateTime.now());
        insertUserToSysUserRequest.setUpdatedAt(LocalDateTime.now());
        adminRepairerMapper.insertIntoSysUser(insertUserToSysUserRequest);
        Long roleId = adminRepairerMapper.findRepairerRoleId();

        if (roleId == null) throw new IllegalStateException("缺少维修人员角色配置");
        InsertUserToSysUserRoleRequest role = new InsertUserToSysUserRoleRequest(insertUserToSysUserRequest.getId(), roleId, insertUserToSysUserRequest.getCreatedAt());
        adminRepairerMapper.insertIntoSysUserRole(role);
        CreateUserResponse response = new CreateUserResponse();
        response.setId(insertUserToSysUserRequest.getId());
        response.setUsername(insertUserToSysUserRequest.getUsername());
        response.setName(insertUserToSysUserRequest.getName());
        response.setRole("REPAIRER");
        response.setEnabled(true);
        return Result.success(response);
    }

    @Override
    public Result updateRepairerStatus(Long id, UpdateStatusRequest updateStatusRequest) {
        if (adminRepairerMapper.selectRepairerById(id) == null) {
            return Result.error("维修员不存在");
        }

        adminRepairerMapper.updateEnabled(id, updateStatusRequest.getEnabled());
        UpdateStatusResponse updateStatusResponse = adminRepairerMapper.getUpdateStatus(id);
        if(updateStatusRequest.getEnabled()==false){
            loginSessionStore.revokeAll(id);
        }
        return Result.success(updateStatusResponse);
    }

    @Override
    public Result getRepairList(Boolean enabled, String keyword) {
        List<RepairManageResponse> data = adminRepairerMapper.getRepairList(enabled,keyword);
        return Result.success(data);
    }

    @Override
    public Result getRepairerDetailed(Long id) {
        RepairManageResponse result = adminRepairerMapper.findRepairerById(id);
        if(result==null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"未找到此维修人员");
        }
        return Result.success(result);
    }
}
