package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.config.CustomLogoutSuccessHandler;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
public class LogoutController {

    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    // 사이드바의 로그아웃 링크(GET /auth/logout)를 기준으로 로그아웃 진입점을 통일한다.
    // 실제 세션/보안 컨텍스트 정리 후 기존 CustomLogoutSuccessHandler로 로컬/소셜 후처리를 이어간다.
    @GetMapping("/auth/logout")
    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) throws IOException, ServletException {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        customLogoutSuccessHandler.onLogoutSuccess(request, response, authentication);
    }
}
