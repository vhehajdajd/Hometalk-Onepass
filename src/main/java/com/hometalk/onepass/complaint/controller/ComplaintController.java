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

    // 등록
    @PreAuthorize("isAuthenticated()")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long register(
            @ModelAttribute ComplaintDto complaintDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {

        return complaintService.saveWithFiles(complaintDto, files, authentication);
    }

    // 상세 조회
    @GetMapping("/{id}")
    public ComplaintDto read(@PathVariable Long id, Authentication authentication) {
        return complaintService.getComplaintDetail(id, authentication);
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

    // 삭제
    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        complaintService.delete(id, authentication);
    }

    // 파일
    @GetMapping("/file/{type}") // type: download or display
    public ResponseEntity<Resource> handleFile(
            @PathVariable String type,
            @RequestParam String fileName,
            @RequestParam(required = false) String originName) throws IOException {

        Path path = validateAndGetPath(fileName);
        Resource resource = new InputStreamResource(Files.newInputStream(path));

        HttpHeaders headers = new HttpHeaders();
        if ("download".equals(type)) {
            String encodedName = UriUtils.encode(originName, StandardCharsets.UTF_8);
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"");
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        } else {
            String contentType = Files.probeContentType(path);
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"));
        }

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    // 경로 조작 공격(Path Traversal) 방지 공통 로직
    private Path validateAndGetPath(String fileName) {
        Path basePath = Paths.get(fileProperties.getPath()).toAbsolutePath().normalize();
        Path path = basePath.resolve(fileName).normalize();
        if (!path.startsWith(basePath) || !Files.exists(path)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 파일 접근입니다.");
        }
        return path;
    }


    // 관리자
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/respond")
    public void respond( // ResponseEntity 대신 void 혹은 업데이트된 DTO 반환
                         @PathVariable Long id,
                         @RequestBody Map<String, String> body,
                         Authentication authentication) {

        String answer = body.get("answer");
        if (answer == null || answer.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변 내용을 입력해주세요.");
        }

        complaintService.respond(id, answer, authentication);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/complete")
    public void complete(@PathVariable Long id, Authentication authentication) {
        complaintService.complete(id, authentication);
    }


}