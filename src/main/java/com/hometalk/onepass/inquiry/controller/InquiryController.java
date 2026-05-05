package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;
    private final LocalAccountRepository localAccountRepository;

    /*
     * 문의 등록
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public Long register(
            @RequestPart("dto") InquiryDto inquiryDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        if (userDetails == null) {
            throw new RuntimeException("로그인이 필요한 서비스입니다.");
        }

        String loginId = userDetails.getUsername();

        var account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자 계정을 찾을 수 없습니다."));

        inquiryDto.setUserId(account.getUser().getId());

        return inquiryService.register(inquiryDto, files, userDetails);
    }

    /*
     * 전체 목록
     */
    @GetMapping
    public Page<InquiryDto> list(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = (userDetails != null) ? userDetails.getUserId() : null;

        boolean isAdmin = userDetails != null &&
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return inquiryService.findAll(userId, isAdmin, pageable);
    }

    /*
     * 내 문의 목록 (보안 구조 수정)
     */
    @GetMapping("/my")
    public Page<InquiryDto> myList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {

        if (userDetails == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        return inquiryService.findByUserId(userDetails.getUserId(), pageable);
    }

    /*
     * 상세 조회 (권한 체크 포함)
     */
    @GetMapping("/{id}")
    public InquiryDto detail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        return inquiryService.getInquiryDetail(id, userDetails);
    }

    /*
     * 답변 (관리자만)
     */
    @PostMapping("/{id}/respond")
    public void respond(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String answer = body.get("answer");

        if (answer == null || answer.trim().isEmpty()) {
            throw new RuntimeException("답변 내용을 입력해주세요.");
        }

        inquiryService.answer(id, answer, userDetails);
    }

    /*
     * 삭제 (작성자 or 관리자)
     */
    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        inquiryService.deleteInquiry(id, userDetails);
    }
}