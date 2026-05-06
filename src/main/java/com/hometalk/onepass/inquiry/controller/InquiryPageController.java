package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
                           @AuthenticationPrincipal CustomUserDetails user,
                           @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = (user != null) ? user.getUserId() : null;

        boolean isAdmin = user != null &&
                user.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<InquiryDto> inquiries = inquiryService.findAll(userId, isAdmin, pageable);

        model.addAttribute("paging", inquiries);
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
    public String registerInquiry(
            @ModelAttribute InquiryDto inquiryDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) throws IOException {

        if (userDetails == null) {
            throw new RuntimeException("로그인 필요");
        }
        inquiryService.register(inquiryDto, files, userDetails);

        return "redirect:/inquiries/list";
    }

    // 4. 문의 상세 페이지 이동
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id,
                             Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {

        InquiryDto inquiryDto = inquiryService.getInquiryDetail(id, userDetails);

        model.addAttribute("inquiry", inquiryDto);
        return "inquiry/inquiryDetail";
    }
}