package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/hometop/inquiries")
@RequiredArgsConstructor
public class InquiryPageController {

    private final InquiryService inquiryService;

    // 1. 문의 목록 페이지
    @GetMapping("/list")
    public String listPage(Model model) {
        System.out.println("🚩 리스트 페이지 컨트롤러 진입 성공!");
        List<InquiryDto> inquiries = inquiryService.findAll();
        model.addAttribute("inquiries", inquiries);


        return "inquiry/inquiryList";
    }

    // 2. 문의 등록 페이지 이동
    @GetMapping("/write")
    public String writePage() {
        return "inquiry/write";
    }

    // 3. 문의 등록 처리 (API가 아닌 페이지 전환용일 경우)
    @PostMapping("/write")
    public String registerInquiry(@ModelAttribute InquiryDto inquiryDto, // Inquiry 대신 InquiryDto 사용
                                  @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {


        inquiryService.register(inquiryDto, files);

        return "redirect:/hometop/inquiries/list";
    }
}