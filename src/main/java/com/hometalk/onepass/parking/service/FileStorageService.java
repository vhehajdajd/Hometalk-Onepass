package com.hometalk.onepass.parking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${file.upload.path}")
    private String uploadPath;

    // 서류 파일 저장 (여러 파일 저장)
    public List<String> saveDocuments(List<MultipartFile> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            List<String> filePaths = new ArrayList<>();
            for (MultipartFile file : documents) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);
                file.transferTo(filePath.toFile());
                filePaths.add(filePath.toString());
            }

            return filePaths;
        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생", e);
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    // 파일 삭제
    public void deleteFile(String filePath) {
        if (filePath == null) return;

        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생", e);
        }
    }
}