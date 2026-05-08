package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InquiryDashboardApiController {

    private final InquiryService inquiryService;

    @GetMapping("/api/inquiry/my-recent")
    public ResponseEntity<List<InquiryDto>> myRecent(Authentication authentication) {
        return ResponseEntity.ok(inquiryService.findMyRecent(authentication));
    }

    @GetMapping("/api/admin/inquiry/recent")
    public ResponseEntity<List<InquiryDto>> adminRecent(Authentication authentication) {
        return ResponseEntity.ok(inquiryService.findAdminRecent(authentication));
    }
}