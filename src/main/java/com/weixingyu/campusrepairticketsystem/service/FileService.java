package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    Result uploadTicketImages(List<MultipartFile> files, Long uploaderId) throws Exception;
}
