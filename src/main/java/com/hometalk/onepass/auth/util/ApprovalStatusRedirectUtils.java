package com.hometalk.onepass.auth.util;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public final class ApprovalStatusRedirectUtils {

    public static final String APPROVAL_NOTICE_ACKED_SESSION_KEY = "APPROVAL_NOTICE_ACKED";

    private ApprovalStatusRedirectUtils() {
    }

    public static Optional<String> getRequiredRedirect(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return Optional.empty();
        }

        User.UserStatus status = userDetails.getStatus();
        if (status == User.UserStatus.PENDING) {
            return Optional.of("/auth/approval/pending");
        }

        if (status == User.UserStatus.REJECTED) {
            return Optional.of("/auth/approval/rejected");
        }

        if (status == User.UserStatus.APPROVED
                && !userDetails.isApprovalNoticeShown()
                && !Boolean.TRUE.equals(request.getSession().getAttribute(APPROVAL_NOTICE_ACKED_SESSION_KEY))) {
            return Optional.of("/auth/approval/approved");
        }

        return Optional.empty();
    }
}
