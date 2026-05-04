package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
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
     * 민원 등록 (POST) - 파일 업로드 통합 버전
     * 주소: POST http://localhost:8090/api/inquiries
     * consumes 설정을 통해 파일 전송(multipart/form-data) 허용
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public Long register(
            @RequestPart("dto") InquiryDto inquiryDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User userDetails) throws IOException {
        if (userDetails == null) {
            throw new RuntimeException("로그인이 필요한 서비스입니다.");
        }
        String loginId = userDetails.getUsername();
        com.hometalk.onepass.auth.entity.LocalAccount account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() -> new RuntimeException("사용자 계정을 찾을 수 없습니다."));

        // account.getUser().getId()로 내 도메인에 필요한 userId를 조용히 가져옵니다.
        inquiryDto.setUserId(account.getUser().getId());

        // 통합된 서비스 메서드 호출 (dto와 files를 같이 넘겨줌)
        return inquiryService.register(inquiryDto, files);
    }

    /*
        전체 민원 목록 조회 (GET)
        관리자나 본인이 작성한 리스트를 볼 때 사용
     */
    @GetMapping
    public Page<InquiryDto> list(@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return inquiryService.findAll(pageable);
    }

    /*
     * 내 문의 목록 조회
     */
    @GetMapping("/my/{userId}")
    public Page<InquiryDto> myLimitList(@PathVariable Long userId,
                                        @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return inquiryService.findByUserId(userId, pageable);
    }

    /*
        상세 조회 (GET)
     */
    @GetMapping("/{id}")
    public InquiryDto detail(@PathVariable Long id) {
        return inquiryService.getInquiryDetail(id);
    }

    /*
        관리자 답변 등록 (POST)
     */
    @PostMapping("/{id}/respond")
    public void respond(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        String answer = body.get("answer");
        if (answer == null || answer.trim().isEmpty()) {
            throw new RuntimeException("답변 내용을 입력해주세요.");
        }
        inquiryService.answer(id, answer);
    }

    /*
        삭제 (DELETE)
     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        inquiryService.deleteInquiry(id);
    }
}