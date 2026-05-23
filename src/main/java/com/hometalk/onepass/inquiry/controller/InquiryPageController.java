package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
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

    @GetMapping("/list")
    public String listPage(Model model,
                           Authentication authentication,
                           @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));

        Long userId = null;

        if (!isAdmin) {
            userId = inquiryService.getLoginUserId(authentication);
        }

        Page<InquiryDto> inquiries = inquiryService.findAll(userId, isAdmin, pageable);

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
    public String registerInquiry(
            @ModelAttribute InquiryDto inquiryDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            Authentication authentication) throws IOException {
        inquiryService.register(inquiryDto, files, authentication);
        return "redirect:/inquiries/list";
    }


    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id,
                             Model model,
                             Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }
        InquiryDto inquiryDto = inquiryService.getInquiryDetail(id, authentication);
        model.addAttribute("inquiry", inquiryDto);
        return "inquiry/inquiryDetail";
    }

    @GetMapping("/my")
    public String myInquiryListPage(Model model,
                                    Authentication authentication,
                                    @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        Long userId = inquiryService.getLoginUserId(authentication);

        Page<InquiryDto> paging = inquiryService.findAll(userId, false, pageable);

        model.addAttribute("paging", paging != null ? paging : Page.empty(pageable));

        return "inquiry/MyInquiryList";
    }
}