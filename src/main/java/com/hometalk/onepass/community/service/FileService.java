package com.hometalk.onepass.community.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {
    // 사진이 저장될 실제 컴퓨터 경로
    @Value("${file.upload.path}")
    private String uploadPath;

    public String storeFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) return null;

        // 1. 파일명 중복 방지를 위한 UUID 생성
        String originalFilename = file.getOriginalFilename();
        String storeFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        // 2. 폴더가 없으면 생성
        File folder = new File(uploadPath);
        if (!folder.exists()) folder.mkdirs();

        // 3. 실제 파일 저장
        file.transferTo(new File(folder, storeFileName));

        return storeFileName; // 저장된 파일명 반환
    }
}