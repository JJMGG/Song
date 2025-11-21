package com.iteima.mysong.main.service.impl;

import com.iteima.mysong.main.service.FileStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class MinioService implements FileStorageService {


    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * 上传本地文件到 MinIO
     * @param filePath 本地文件路径（如 "E:/temp/example.mp3"）
     * @param objectName MinIO 存储的文件名（如 "music/example.mp3"）
     */
    public void uploadLocalFile(String filePath, String objectName)
            throws IOException, MinioException {
        File file = new File(filePath);
        String contentType = determineContentType(file.getName()); // 根据文件名自动识别类型
        try (InputStream stream = new FileInputStream(file)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, file.length(), -1)
                            .contentType(contentType) // 关键：设置正确的 Content-Type
                            .build()
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传 Spring MultipartFile（适用于 HTTP 文件上传）
     */
    public void uploadMultipartFile(MultipartFile file, String objectName)
            throws IOException, MinioException {
        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = determineContentType(file.getOriginalFilename()); // 备用方案
        }
        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, file.getSize(), -1)
                            .contentType(contentType) // 确保 MultipartFile 的 Content-Type 正确
                            .build()
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }
    private String determineContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "mp3":
                return "audio/mpeg";
            case "mp4":
                return "video/mp4";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            default:
                return "application/octet-stream";
        }
    }


    @Override
    public String uploadFile(MultipartFile file) {
        try {
            // 检查存储桶是否存在
            System.out.println("进入预处理");

            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String objectName = UUID.randomUUID() + fileExtension;

            // 上传文件到MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // 返回文件访问URL（需确保MinIO配置了外部访问）
            return String.format("%s/%s/%s",
                    "http://192.168.3.226:9000", // 替换为你的MinIO服务地址
                    bucketName,
                    objectName);
        } catch (Exception e) {

            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }
}
