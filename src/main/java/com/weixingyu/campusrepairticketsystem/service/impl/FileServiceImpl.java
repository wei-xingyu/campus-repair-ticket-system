package com.weixingyu.campusrepairticketsystem.service.impl;

import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.pojo.response.UploadTicketImagesResponse;
import com.weixingyu.campusrepairticketsystem.service.FileService;
import com.weixingyu.campusrepairticketsystem.utils.AliyunOSSOperator;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileServiceImpl implements FileService {
    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    @Resource
    private AliyunOSSOperator aliyunOSSOperator;

    @Override
    public Result uploadTicketImages(List<MultipartFile> files, Long uploaderId) throws Exception {



        // 先完整校验所有文件，再开始上传，避免前几个成功后才发现后面的文件不合法。
        List<ValidatedImage> validatedImages = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            Result validation = validateImage(file, validatedImages);
            if (validation != null) {
                return validation;
            }
        }

        List<String> urls = new ArrayList<>(validatedImages.size());
        for (ValidatedImage image : validatedImages) {
            try {
                urls.add(aliyunOSSOperator.upload(image.content(), image.suffix(), uploaderId));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "图片存储服务上传失败，请稍后重试", e);
            }
        }
        return Result.success(new UploadTicketImagesResponse(urls));
    }

    private Result validateImage(MultipartFile file, List<ValidatedImage> validatedImages) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.error("图片不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("每张图片不能超过5MB");
        }

        String originalName = file.getOriginalFilename();
        int dot = originalName == null ? -1 : originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return Result.error("仅允许JPEG、PNG、WebP图片");
        }
        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        String expectedContentType = ALLOWED_TYPES.get(extension);
        if (expectedContentType == null || !expectedContentType.equalsIgnoreCase(file.getContentType())) {
            return Result.error("图片扩展名与媒体类型不匹配");
        }

        byte[] content = file.getBytes();
        if (!hasValidImageSignature(extension, content)) {
            return Result.error("文件内容不是有效的JPEG、PNG或WebP图片");
        }
        validatedImages.add(new ValidatedImage(content, "." + extension));
        return null;
    }

    private boolean hasValidImageSignature(String extension, byte[] bytes) {
        return switch (extension) {
            case "jpg", "jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
            case "png" -> bytes.length >= 8
                    && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                    && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case "webp" -> bytes.length >= 12
                    && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                    && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            default -> false;
        };
    }

    private record ValidatedImage(byte[] content, String suffix) {
    }
}
