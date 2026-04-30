package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.ComplaintDto;
import com.hometalk.onepass.inquiry.entity.Complaint;
import com.hometalk.onepass.inquiry.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    /**
     * 민원 등록 (글 + 파일 통합)
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public Long register(
            @RequestPart("dto") ComplaintDto complaintDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {
        return complaintService.saveWithFiles(complaintDto, files);
    }

    /**
     * 전체 민원 목록 조회
     */
    @GetMapping
    public List<ComplaintDto> list() {
        return complaintService.findAll();
    }

    /**
     * 내 민원 목록 조회 (지현님이 말씀하신 기능)
     */
    @GetMapping("/my/{userId}")
    public List<ComplaintDto> myLimitList(@PathVariable("userId") Long userId) {
        return complaintService.findByUserId(userId);
    }

    /**
     * 상세 조회
     */
    @GetMapping("/{id}")
    public ComplaintDto detail(@PathVariable("id") Long id) {
        Complaint complaint = complaintService.findOne(id);
        return ComplaintDto.fromEntity(complaint);
    }

    /**
     * 삭제
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        complaintService.delete(id);
    }

    /**
     * 첨부파일 다운로드
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String fileName, @RequestParam String originName) throws IOException {
        Path path = Paths.get("C:/onepass/complaint_uploads/" + fileName);
        Resource resource = new InputStreamResource(Files.newInputStream(path));

        String encodedName = UriUtils.encode(originName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .body(resource);
    }

    /**
     * 이미지 미리보기용 출력
     */
    @GetMapping("/display")
    public ResponseEntity<Resource> display(@RequestParam String fileName) throws IOException {
        Path path = Paths.get("C:/onepass/complaint_uploads/" + fileName);
        Resource resource = new InputStreamResource(Files.newInputStream(path));

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG) // 이미지 형식에 따라 조정 가능
                .body(resource);
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<String> respond(@PathVariable("id") Long id, @RequestBody String respond) {
        complaintService.respond(id, respond);
        return ResponseEntity.ok("답변 등록 완료");
    }

}