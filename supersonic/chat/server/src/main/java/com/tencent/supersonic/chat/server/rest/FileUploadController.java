package com.tencent.supersonic.chat.server.rest;

import com.tencent.supersonic.auth.api.authentication.utils.UserHolder;
import com.tencent.supersonic.common.pojo.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

/**
 * 上传文件接口
 *
 * @author JiangWeiWei
 */
@RestController
@RequestMapping("/supersonic/api")
public class FileUploadController {

    @Value("${file.upload-dir: D:/temporary}")
    private String uploadDir;

    /**
     * 文件上传
     * @param file  上传文件
     * @return      上传文件路径
     */
    @PostMapping("/fileUpload")
    public String handleFileUpload(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = Optional.ofNullable(UserHolder.findUser(request, response))
                .orElseThrow(() -> new SecurityException("用户未认证"));

        validateFile(file);

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            // 自动创建目录
            Files.createDirectories(uploadPath);

            String sanitizedFileName = sanitizeFileName(file.getOriginalFilename());
            Path targetLocation = uploadPath.resolve(sanitizedFileName);

            // 使用NIO进行文件存储并设置文件属性
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 返回相对路径或可访问的URL路径
            return targetLocation.getFileName().toString();

        } catch (IOException ex) {
            throw new RuntimeException("文件上传失败", ex);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }

        String contentType = file.getContentType();
        if (contentType != null && (!contentType.startsWith("application/vnd.openxmlformats"))) {
            throw new RuntimeException("不支持的文件类型：" + contentType);
        }
    }

    private String sanitizeFileName(String originalFilename) {
        return Optional.ofNullable(originalFilename)
                .filter(name -> !name.isEmpty())
                .map(name -> {
                    String uuid = UUID.randomUUID().toString().replace("-", "");
                    String extension = "";
                    int lastDotIndex = name.lastIndexOf('.');
                    if (lastDotIndex > 0) {
                        extension = name.substring(lastDotIndex).toLowerCase();
                    }
                    return uuid + extension;
                })
                .orElseThrow(() -> new RuntimeException("无效的文件名"));
    }
}