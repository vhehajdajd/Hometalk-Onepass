package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.complaint.config.FileProperties;
import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final FileProperties fileProperties;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long register(
            @ModelAttribute ComplaintDto complaintDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        return complaintService.saveWithFiles(complaintDto, files, authentication);
    }

    @GetMapping
    public Page<ComplaintDto> list(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = null;
        boolean isAdmin = false;

        if (authentication != null && authentication.isAuthenticated()) {
            userId = complaintService.getLoginUserId(authentication);
            isAdmin = complaintService.isAdmin(authentication);
        }

        return complaintService.findAll(userId, isAdmin, pageable);
    }

    @GetMapping("/my")
    public Page<ComplaintDto> myLimitList(
            Authentication authentication,
            Pageable pageable) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인 필요");
        }

        Long userId = complaintService.getLoginUserId(authentication);

        return complaintService.findByUserId(userId, pageable);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        complaintService.delete(id, authentication);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String fileName,
            @RequestParam String originName) throws IOException {

        Path basePath = Paths.get(fileProperties.getPath()).toAbsolutePath().normalize();
        Path path = basePath.resolve(fileName).normalize();

        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("잘못된 파일 경로");
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("파일 없음");
        }

        Resource resource = new InputStreamResource(Files.newInputStream(path));
        String encodedName = UriUtils.encode(originName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedName + "\"")
                .body(resource);
    }

    @GetMapping("/display")
    public ResponseEntity<Resource> display(@RequestParam String fileName) throws IOException {

        Path basePath = Paths.get(fileProperties.getPath()).toAbsolutePath().normalize();
        Path path = basePath.resolve(fileName).normalize();

        if (!path.startsWith(basePath)) {
            throw new IllegalArgumentException("잘못된 파일 경로");
        }

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("파일 없음");
        }

        Resource resource = new InputStreamResource(Files.newInputStream(path));

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/respond")
    public ResponseEntity<String> respond(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String answer = body.get("answer");

        if (answer == null || answer.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        complaintService.respond(id, answer, authentication);

        return ResponseEntity.ok("답변 등록 완료");
    }
}