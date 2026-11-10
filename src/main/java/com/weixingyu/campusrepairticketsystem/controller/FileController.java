package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.FileService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestAttribute;
import jakarta.validation.constraints.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
public class FileController {
    @Resource
    private FileService fileService;

    @RequireRole({"STUDENT", "REPAIRER", "ADMIN"})
    @PostMapping(value = "/ticket-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadTicketImages(@RequestParam("files") @NotEmpty(message="请至少选择一张图片") @Size(max=5,message="一次最多上传5张图片") List<MultipartFile> files,
                                     @RequestAttribute("loginUser") LoginUser loginUser) throws Exception {
        return fileService.uploadTicketImages(files, loginUser.getId());
    }
}
