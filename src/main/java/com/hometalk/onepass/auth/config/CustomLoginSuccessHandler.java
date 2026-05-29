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
            // React에서 호출한 API 로그인은 redirect 응답을 직접 따라가게 하지 않고,
            // 이동해야 할 경로를 JSON으로 내려서 프론트가 location 이동을 결정하게 한다.
            if (isApiLoginRequest(request)) {
                writeApiLoginSuccess(response, approvalStatusRedirect);
                return;
            }

            getRedirectStrategy().sendRedirect(request, response, approvalStatusRedirect);
            return;
        }

        String redirectUrl = LoginRedirectUtils.consumeRedirectUrl(request)
                .orElse("/dashboard");

        if (isApiLoginRequest(request)) {
            // 기존 HTML form 로그인은 redirect를 유지하고, React API 로그인만 JSON으로 응답한다.
            writeApiLoginSuccess(response, redirectUrl);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    private void writeApiLoginSuccess(HttpServletResponse response, String redirectUrl) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"redirectUrl\":\"" + redirectUrl + "\"}");
    }

    private boolean isApiLoginRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        return "/api/auth/login".equals(path);
    }
}
