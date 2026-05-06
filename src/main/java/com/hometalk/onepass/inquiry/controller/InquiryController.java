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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
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

    @GetMapping("/list")
    public String listPage(Model model,
                           @AuthenticationPrincipal CustomUserDetails user,
                           @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = null;
        boolean isAdmin = false;

        if (user != null) {
            userId = user.getUserId();
            isAdmin = user.getAuthorities().stream()
                    .anyMatch(a ->
                            a.getAuthority().equals("ROLE_ADMIN")
                                    || a.getAuthority().equals("ADMIN")
                    );
        }

        Page<InquiryDto> inquiries = inquiryService.findAll(userId, isAdmin, pageable);

        model.addAttribute("inquiries", inquiries);
        model.addAttribute("currentPage", inquiries.getNumber());
        model.addAttribute("totalPages", inquiries.getTotalPages());

        return "inquiry/inquiryList";
    }

    @GetMapping("/write")
    public String writePage() {
        return "inquiry/inquiryWrite";
    }

    @PostMapping("/write")
    public String registerInquiry(
            @ModelAttribute InquiryDto inquiryDto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {

        if (userDetails == null) {
            return "redirect:/auth";
        }

        inquiryService.register(inquiryDto, files, userDetails);

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
}