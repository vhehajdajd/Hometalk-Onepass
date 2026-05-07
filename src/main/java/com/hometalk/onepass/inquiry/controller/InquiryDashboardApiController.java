/*
package com.hometalk.onepass.admin.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiry")
public class AdminInquiryApiController {

    private final InquiryService inquiryService;

    @GetMapping("/recent")
    public ResponseEntity<List<InquiryDto>> recent(Authentication authentication) {
        return ResponseEntity.ok(inquiryService.findAdminRecent(authentication));
    }
}*/
