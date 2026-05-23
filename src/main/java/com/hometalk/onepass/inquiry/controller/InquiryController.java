package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;


    /*
     * 민원 등록 (POST) - 파일 업로드 통합 버전
     * 주소: POST http://localhost:8090/api/inquiries
     * consumes 설정을 통해 파일 전송(multipart/form-data)을 허용합니다.
     */
    @PostMapping(consumes = {"multipart/form-data"})
    public Long register(
            @RequestPart("dto") InquiryDto inquiryDto,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) throws IOException {

        // 통합된 서비스 메서드 호출 (dto와 files를 같이 넘겨줌)
        return inquiryService.register(inquiryDto, files);
    }

    /*
        전체 민원 목록 조회 (GET)
        관리자나 본인이 작성한 리스트를 볼 때 사용
     */
    @GetMapping
    public List<InquiryDto> list() {
        return inquiryService.findAll();
    }

    /*
        상세 조회 (GET)

     */
    @GetMapping("/{id}")
    public InquiryDto detail(@PathVariable("id") Long id) { // 괄호 오타 수정
        Inquiry inquiry = inquiryService.findOne(id);
        return InquiryDto.fromEntity(inquiry);
    }

    /*
        삭제 (DELETE)

     */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        inquiryService.deleteInquiry(id);
    }
}
