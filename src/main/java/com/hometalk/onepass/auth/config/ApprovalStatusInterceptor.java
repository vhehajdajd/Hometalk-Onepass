package com.hometalk.onepass.auth.config;

import com.hometalk.onepass.auth.util.ApprovalStatusRedirectUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApprovalStatusInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI().substring(request.getContextPath().length());
        if (isAllowedPath(requestPath)) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String redirectPath = ApprovalStatusRedirectUtils.getRequiredRedirect(authentication, request).orElse(null);
        if (redirectPath == null || redirectPath.equals(requestPath)) {
            return true;
        }

        response.sendRedirect(request.getContextPath() + redirectPath);
        return false;
    }

    private boolean isAllowedPath(String requestPath) {
        return requestPath.startsWith("/auth/approval/")
                || requestPath.equals("/auth/logout")
                || requestPath.startsWith("/css/")
                || requestPath.startsWith("/js/")
                || requestPath.startsWith("/images/")
                || requestPath.startsWith("/auth/loginimage/");
    }
}
