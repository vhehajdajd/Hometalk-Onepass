package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.util.ApprovalStatusRedirectUtils;
import com.hometalk.onepass.auth.util.LoginRedirectUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    // 일반 로그인 성공 후 홈 화면에서 요청했던 목적지가 있으면 해당 화면으로 보낸다.
    // 목적지가 없으면 기본 홈 화면인 /home으로 이동한다.
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String approvalStatusRedirect = ApprovalStatusRedirectUtils.getRequiredRedirect(authentication, request)
                .orElse(null);
        if (approvalStatusRedirect != null) {
            getRedirectStrategy().sendRedirect(request, response, approvalStatusRedirect);
            return;
        }

        String redirectUrl = LoginRedirectUtils.consumeRedirectUrl(request)
                .orElse("/home");

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
