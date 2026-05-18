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

    public List<String> saveDocuments(List<MultipartFile> documents) {
        if (documents == null || documents.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> filePaths = new ArrayList<>();

        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            for (MultipartFile file : documents) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Path filePath = uploadDir.resolve(fileName);
                file.transferTo(filePath.toFile());
                filePaths.add(filePath.toString());
            }

            return filePaths;

        } catch (IOException e) {
            log.error("파일 저장 중 오류 발생", e);

            // 저장 실패 시 이미 저장된 파일 롤백 처리
            for (String savedPath : filePaths) {
                try {
                    Files.deleteIfExists(Paths.get(savedPath));
                    log.info("롤백 - 파일 삭제 완료: {}", savedPath);
                } catch (IOException deleteException) {
                    log.error("롤백 실패 - 파일 삭제 오류: {}", savedPath, deleteException);
                }
            }

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