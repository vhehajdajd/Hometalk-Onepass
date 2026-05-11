package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.complaint.config.FileProperties;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;
    private final FileProperties fileProperties;

    @GetMapping("/my-recent")
    public ResponseEntity<List<InquiryDto>> myRecent(Authentication authentication) {
        return ResponseEntity.ok(inquiryService.findMyRecent(authentication));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerInquiry(
            @ModelAttribute InquiryDto inquiryDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {

        inquiryService.register(inquiryDto, files, authentication);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/file/display")
    public ResponseEntity<Resource> displayFile(@RequestParam String fileName)
            throws MalformedURLException {

        String uploadPath = fileProperties.getPath();

        Path filePath = Paths.get(uploadPath).resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    @GetMapping("/file/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String fileName,
            @RequestParam String originName) throws MalformedURLException {

        String uploadPath = fileProperties.getPath();

        Path filePath = Paths.get(uploadPath).resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                java.net.URLEncoder.encode(
                                        originName,
                                        java.nio.charset.StandardCharsets.UTF_8
                                ) + "\""
                )
                .body(resource);
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<String> respond(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String answer = body.get("answer");

        if (answer == null || answer.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("답변 내용을 입력해주세요.");
        }

        inquiryService.answer(id, answer, authentication);

        return ResponseEntity.ok("답변 등록 완료");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInquiry(
            @PathVariable Long id,
            Authentication authentication
    ) {
        inquiryService.deleteInquiry(id, authentication);
        return ResponseEntity.noContent().build();
    }


}