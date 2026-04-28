package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.MinioUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Tag(name = "通用接口")
@RestController
@Slf4j
@RequestMapping("/admin/common")
public class CommonController {
    @Autowired
    private MinioUtil minioUtil;
    @PostMapping("/upload")
    @Operation(summary="上传文件")
    public Result<String> upload(MultipartFile file) {
        log.info("文件上传：{}", file);
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String  extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID().toString() + extension;

            // 上传到 MinIO
            String filePath = minioUtil.upload(file.getBytes(), objectName);

            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}", e);
        }
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
