package com.hometalk.onepass.community.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileService {

    @Value("${file.upload.path}")
    private String uploadPath;

    // 이미지 파일 저장
    public String storeFile(MultipartFile file) throws IOException  {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            Path uploadDir = Paths.get(uploadPath);
            // 업로드 폴더 없으면 생성
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            // 원본 파일명
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            // 확장자 추출
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            // 저장 파일명 생성
            String storeFileName = UUID.randomUUID() + ext;
            // 저장 경로
            Path filePath = uploadDir.resolve(storeFileName);
            // 파일 저장
            file.transferTo(filePath.toFile());
            return storeFileName;

        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생", e);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    // 파일 삭제
    public void deleteFile(String filePath) {
        if (filePath == null) return;
        try {
            Path path = Paths.get(uploadPath,
                    filePath.replace("/uploads/", ""));
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("파일 삭제 실패", e);
        }
    }
}