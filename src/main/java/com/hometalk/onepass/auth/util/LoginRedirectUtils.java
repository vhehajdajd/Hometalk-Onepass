package com.hometalk.onepass.auth.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public final class LoginRedirectUtils {

    public static final String REDIRECT_URL_SESSION_KEY = "LOGIN_REDIRECT_URL";

    private LoginRedirectUtils() {
    }

    // 로그인 전 사용자가 가려던 내부 경로를 세션에 저장한다.
    // RedirectStrategy가 context-path를 다시 붙이므로 세션에는 /hometop을 제거한 경로만 저장한다.
    public static void saveRedirectUrl(HttpServletRequest request, String redirectUrl) {
        Optional<String> normalizedUrl = normalizeRedirectUrl(request, redirectUrl);
        if (normalizedUrl.isEmpty()) {
            return;
        }

        request.getSession().setAttribute(REDIRECT_URL_SESSION_KEY, normalizedUrl.get());
    }

    // 로그인 성공 후 한 번만 사용하고 세션에서 제거한다.
    public static Optional<String> consumeRedirectUrl(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }

        Object redirectUrl = session.getAttribute(REDIRECT_URL_SESSION_KEY);
        session.removeAttribute(REDIRECT_URL_SESSION_KEY);

        if (!(redirectUrl instanceof String value)) {
            return Optional.empty();
        }

        return normalizeRedirectUrl(request, value);
    }

    private static Optional<String> normalizeRedirectUrl(HttpServletRequest request, String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) {
            return Optional.empty();
        }

        String value = redirectUrl.trim();
        if (!value.startsWith("/") || value.startsWith("//")) {
            return Optional.empty();
        }

        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank()) {
            while (value.equals(contextPath) || value.startsWith(contextPath + "/")) {
                if (value.equals(contextPath)) {
                    value = "/";
                    break;
                }

                value = value.substring(contextPath.length());
            }
        }

        return Optional.of(value);
    }
}
