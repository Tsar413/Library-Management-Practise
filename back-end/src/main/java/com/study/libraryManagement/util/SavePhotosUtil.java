package com.study.libraryManagement.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class SavePhotosUtil {
    public static String savePhoto(MultipartFile file, String serverPath, String localPath){
        String name = file.getOriginalFilename();
        String filename = getFileName(name);
        File directory = new File(localPath);
        File fileHome = new File(directory, filename);
        try {
            file.transferTo(fileHome);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return serverPath + "/" + filename;
    }

    private static String getFileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("图片文件名无效");
        }
        int index = name.lastIndexOf(".");
        if (index < 0) {
            throw new RuntimeException("图片文件缺少扩展名");
        }
        // 获取扩展名，例如 .jpg
        String suffix = name.substring(index).toLowerCase();
        // 限制上传文件类型
        if (!".jpg".equals(suffix)
                && !".jpeg".equals(suffix)
                && !".png".equals(suffix)
                && !".webp".equals(suffix)) {
            throw new RuntimeException("只允许上传图片文件");
        }
        // 使用 UUID 重命名，避免文件重名
        return UUID.randomUUID().toString() + suffix;
    }
}
