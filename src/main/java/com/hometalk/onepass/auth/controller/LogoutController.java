package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.config.CustomLogoutSuccessHandler;
import com.hometalk.onepass.auth.config.RememberMeConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class LogoutController {

    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    @GetMapping("/auth/oauth2/kakao/logout")
    public void kakaoLogout(@RequestParam("client_id") String clientId,
                            @RequestParam("logout_redirect_uri") String logoutRedirectUri,
                            HttpServletResponse response) throws IOException {
        String kakaoLogoutUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/logout")
                .queryParam("client_id", clientId)
                .queryParam("logout_redirect_uri", logoutRedirectUri)
                .build()
                .toUriString();

        response.sendRedirect(kakaoLogoutUrl);
    }

    // 사이드바의 로그아웃 링크(GET /auth/logout)를 기준으로 로그아웃 진입점을 통일한다.
    // 실제 세션/보안 컨텍스트 정리 후 기존 CustomLogoutSuccessHandler로 로컬/소셜 후처리를 이어간다.
    @GetMapping("/auth/logout")
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) throws IOException, ServletException {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        logoutHandler.logout(request, response, authentication);
        SecurityContextHolder.clearContext();
        expireCookie(response, "JSESSIONID", request.getContextPath());
        expireCookie(response, RememberMeConfig.REMEMBER_ME_PARAMETER, request.getContextPath());
        expireCookie(response, "JSESSIONID", "/");
        expireCookie(response, RememberMeConfig.REMEMBER_ME_PARAMETER, "/");
        customLogoutSuccessHandler.onLogoutSuccess(request, response, authentication);
    }

    private void expireCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path == null || path.isBlank() ? "/" : path);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
