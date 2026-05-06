package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    /*
     * 민원 등록
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long register(
            @RequestPart("dto") ComplaintDto complaintDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails user) throws IOException {

        return complaintService.saveWithFiles(complaintDto, files, user);
    }

    /*
     * 전체 목록 (비밀글 제외 + 관리자 전체)
     */
    @GetMapping
    public Page<ComplaintDto> list(
            @AuthenticationPrincipal CustomUserDetails user,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = (user != null) ? user.getUserId() : null;

        boolean isAdmin = user != null &&
                user.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return complaintService.findAll(userId, isAdmin, pageable);
    }

    /*
     * 내 민원
     */
    @GetMapping("/my")
    public Page<ComplaintDto> myLimitList(
            @AuthenticationPrincipal CustomUserDetails user,
            Pageable pageable) {

        if (user == null || user.getUserId() == null) {
            throw new AccessDeniedException("로그인 필요");
        }

        return complaintService.findByUserId(user.getUserId(), pageable);
    }

    /*
     * 삭제 (작성자 or 관리자)
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       @AuthenticationPrincipal CustomUserDetails user) {

        complaintService.delete(id, user);
    }

    /*
     * 파일 다운로드
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String fileName,
            @RequestParam String originName) throws IOException {

        Path basePath = Paths.get(fileProperties.getUploadPath()).toAbsolutePath().normalize();
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

    /*
     * 이미지 미리보기
     */
    @GetMapping("/display")
    public ResponseEntity<Resource> display(@RequestParam String fileName) throws IOException {

        Path basePath = Paths.get(fileProperties.getUploadPath()).toAbsolutePath().normalize();
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

    /*
     * 관리자 답변
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/respond")
    public ResponseEntity<String> respond(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {

        String answer = body.get("answer");
        if (answer == null || answer.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        complaintService.respond(id, answer, user);
        return ResponseEntity.ok("답변 등록 완료");
    }
}