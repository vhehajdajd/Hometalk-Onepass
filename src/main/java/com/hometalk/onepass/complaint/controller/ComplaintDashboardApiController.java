package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ComplaintDashboardApiController {

    private final ComplaintService complaintService;

    @GetMapping("/api/complaint/my-recent")
    public ResponseEntity<List<ComplaintDto>> myRecent(Authentication authentication) {
        return ResponseEntity.ok(complaintService.findMyRecent(authentication));
    }

    @GetMapping("/api/admin/complaint/recent")
    public ResponseEntity<List<ComplaintDto>> adminRecent(Authentication authentication) {
        return ResponseEntity.ok(complaintService.findAdminRecent(authentication));
    }
}