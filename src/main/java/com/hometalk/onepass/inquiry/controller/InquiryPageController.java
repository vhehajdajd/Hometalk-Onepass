package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/inquiries")
@RequiredArgsConstructor
public class InquiryPageController {

    private final InquiryService inquiryService;

    // 1. 문의 목록 페이지
    @GetMapping("/list")
    public String listPage(Model model,
                           @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        System.out.println("🚩 리스트 페이지 컨트롤러 진입 성공!");
        Page<InquiryDto> inquiries = inquiryService.findAll(pageable);
        model.addAttribute("inquiries", inquiries);
        model.addAttribute("currentPage", inquiries.getNumber());
        model.addAttribute("totalPages", inquiries.getTotalPages());

        return "inquiry/inquiryList";
    }

    // 2. 문의 등록 페이지 이동
    @GetMapping("/write")
    public String writePage() {
        return "inquiry/inquiryWrite";
    }

    // 3. 문의 등록 처리 (API가 아닌 페이지 전환용일 경우)
    @PostMapping("/write")
    public String registerInquiry(@ModelAttribute InquiryDto inquiryDto, // Inquiry 대신 InquiryDto 사용
                                  @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {

        inquiryService.register(inquiryDto, files);

        return "redirect:/inquiries/list";
    }

    // 4. 문의 상세 페이지 이동
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        InquiryDto inquiryDto = inquiryService.getInquiryDetail(id);
        model.addAttribute("inquiry", inquiryDto);
        return "inquiry/inquiryDetail";
    }
}