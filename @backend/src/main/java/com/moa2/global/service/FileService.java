package com.moa2.global.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.max-size:10485760}") // 10MB 기본값
    private long maxFileSize;

    /**
     * 단일 파일 업로드 (포스터 이미지)
     * @param file 업로드할 파일
     * @param subDirectory 서브 디렉토리 (예: "posters", "details")
     * @return 상대 경로 (예: "/uploads/posters/2024/01/15/uuid-filename.jpg")
     */
    public String uploadFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }

        validateFile(file);

        try {
            // 날짜별 디렉토리 생성 (예: 2024/01/15)
            LocalDate today = LocalDate.now();
            String datePath = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            
            // 전체 경로: uploads/{subDirectory}/{yyyy}/{MM}/{dd}/
            Path uploadPath = Paths.get(uploadDir, subDirectory, datePath);
            
            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // UUID를 사용하여 파일명 중복 방지
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 상대 경로 반환 (예: /uploads/posters/2024/01/15/uuid-filename.jpg)
            String relativePath = "/" + uploadDir + "/" + subDirectory + "/" + datePath + "/" + fileName;
            log.info("파일 업로드 성공: {}", relativePath);
            
            return relativePath;
        } catch (IOException e) {
            log.error("파일 업로드 실패: {}", e.getMessage(), e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 여러 파일 업로드 (상세 이미지)
     * @param files 업로드할 파일 리스트
     * @param subDirectory 서브 디렉토리
     * @return 상대 경로 배열
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
     * @param relativePath 상대 경로 (예: /uploads/posters/2024/01/15/uuid-filename.jpg)
     */
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }

        try {
            // 상대 경로에서 첫 번째 "/" 제거 후 전체 경로 구성
            String pathWithoutLeadingSlash = relativePath.startsWith("/") 
                ? relativePath.substring(1) 
                : relativePath;
            Path filePath = Paths.get(pathWithoutLeadingSlash);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("파일 삭제 성공: {}", relativePath);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: {}", relativePath);
            }
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", e.getMessage(), e);
            // 삭제 실패해도 예외를 던지지 않음 (이미 삭제된 경우 등)
        }
    }

    /**
     * 여러 파일 삭제
     * @param relativePaths 상대 경로 배열
     */
    public void deleteFiles(String[] relativePaths) {
        if (relativePaths == null) {
            return;
        }
        
        for (String path : relativePaths) {
            deleteFile(path);
        }
    }

    /**
     * 파일 유효성 검증
     * @param file 검증할 파일
     */
    private void validateFile(MultipartFile file) {
        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                String.format("파일 크기가 너무 큽니다. 최대 크기: %d bytes", maxFileSize)
            );
        }

        // 파일 확장자 검증 (이미지 파일만 허용)
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

        // Content-Type 검증
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다");
        }
    }
}

