package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.service.UserApprovalStatusService;
import com.hometalk.onepass.auth.util.ApprovalStatusRedirectUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AuthApprovalStatusController {

    private final UserApprovalStatusService userApprovalStatusService;

    @GetMapping("/auth/approval/pending")
    public String pending() {
        return "auth/approval-pending";
    }

    @GetMapping("/auth/approval/rejected")
    public String rejected() {
        return "auth/approval-rejected";
    }

    @GetMapping("/auth/approval/approved")
    public String approved(@AuthenticationPrincipal CustomUserDetails userDetails,
                           HttpServletRequest request) {
        if (userDetails != null) {
            userApprovalStatusService.markApprovalNoticeShown(userDetails.getUserId());
            request.getSession().setAttribute(ApprovalStatusRedirectUtils.APPROVAL_NOTICE_ACKED_SESSION_KEY, true);
        }

        return "auth/approval-approved";
    }
}
