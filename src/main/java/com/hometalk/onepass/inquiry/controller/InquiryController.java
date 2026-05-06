package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerInquiry(
            @ModelAttribute InquiryDto inquiryDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {
        inquiryService.register(inquiryDto, files, authentication);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<String> respond(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body, // JSON 데이터를 받기 위한 Map
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
}