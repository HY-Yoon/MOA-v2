package com.moa2.global.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${file.upload.provider:local}")
    private String provider;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:52428800}") // 50MB 기본값
    private long maxFileSize;

    @Value("${aws.s3.bucket:}")
    private String bucketName;

    @Value("${aws.cloudfront.domain:}")
    private String cloudfrontDomain;

    @Value("${aws.region:ap-northeast-2}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        if ("s3".equalsIgnoreCase(provider)) {
            try {
                this.s3Client = S3Client.builder()
                        .region(Region.of(region))
                        .build();
                log.info("S3Client initialized successfully for bucket: {}", bucketName);
            } catch (Exception e) {
                log.error("Failed to initialize S3Client: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 단일 파일 업로드
     */
    public String uploadFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        validateFile(file);

        LocalDate today = LocalDate.now();
        String datePath = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;
        
        String key = subDirectory + "/" + datePath + "/" + fileName;

        if ("s3".equalsIgnoreCase(provider) && s3Client != null) {
            return uploadToS3(file, key);
        } else {
            return uploadToLocal(file, subDirectory, datePath, fileName);
        }
    }

    private String uploadToS3(MultipartFile file, String key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            String cloudfrontUrl = "https://" + cloudfrontDomain + "/" + key;
            log.info("S3 파일 업로드 성공: {}", cloudfrontUrl);
            return cloudfrontUrl;
        } catch (IOException e) {
            log.error("S3 파일 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("S3 파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    private String uploadToLocal(MultipartFile file, String subDirectory, String datePath, String fileName) {
        try {
            Path uploadPath = Paths.get(uploadDir, subDirectory, datePath);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = "/" + uploadDir + "/" + subDirectory + "/" + datePath + "/" + fileName;
            log.info("로컬 파일 업로드 성공: {}", relativePath);
            return relativePath;
        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("로컬 파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 여러 파일 업로드
     */
    public String[] uploadFiles(List<MultipartFile> files, String subDirectory) {
        if (files == null || files.isEmpty()) {
            return new String[0];
        }

        List<String> paths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                paths.add(uploadFile(file, subDirectory));
            }
        }

        return paths.toArray(new String[0]);
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        if ("s3".equalsIgnoreCase(provider) && s3Client != null && path.startsWith("https://" + cloudfrontDomain)) {
            try {
                String key = path.substring(("https://" + cloudfrontDomain + "/").length());
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
                log.info("S3 파일 삭제 성공: {}", key);
            } catch (Exception e) {
                log.error("S3 파일 삭제 실패: {}", e.getMessage(), e);
            }
        } else {
            try {
                String pathWithoutLeadingSlash = path.startsWith("/") ? path.substring(1) : path;
                Path filePath = Paths.get(pathWithoutLeadingSlash);
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("로컬 파일 삭제 성공: {}", path);
                } else {
                    log.warn("삭제할 파일이 존재하지 않습니다: {}", path);
                }
            } catch (IOException e) {
                log.error("로컬 파일 삭제 실패: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 여러 파일 삭제
     */
    public void deleteFiles(String[] paths) {
        if (paths == null) {
            return;
        }

        for (String path : paths) {
            deleteFile(path);
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFile(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(String.format("파일 크기가 너무 큽니다. 최대 크기: %d bytes", maxFileSize));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다");
        }

        String extension = originalFilename.toLowerCase();
        if (!extension.endsWith(".jpg") && !extension.endsWith(".jpeg")
                && !extension.endsWith(".png") && !extension.endsWith(".gif")
                && !extension.endsWith(".webp")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다 (jpg, jpeg, png, gif, webp)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다");
        }
    }
}
