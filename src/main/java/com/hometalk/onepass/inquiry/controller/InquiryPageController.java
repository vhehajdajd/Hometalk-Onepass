package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
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
    public String listPage(Model model) {
        List<InquiryDto> inquiries = inquiryService.findAll();
        model.addAttribute("inquiries", inquiries);


        return "inquiry/inquiryList";
    }


    @GetMapping("/write")
    public String writePage() {

        return "inquiry/complaintWrite"; //
    }
    // 3. 문의 등록 처리
    @PostMapping("/write")
    public String registerInquiry(@ModelAttribute InquiryDto inquiryDto, // Inquiry 대신 InquiryDto 사용
                                  @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {


        inquiryService.register(inquiryDto, files);

        return "redirect:/hometop/inquiries/list";
    }
}