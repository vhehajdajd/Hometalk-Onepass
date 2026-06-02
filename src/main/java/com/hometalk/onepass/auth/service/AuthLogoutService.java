package com.hometalk.onepass.auth.service;

import com.hometalk.onepass.auth.config.RememberMeConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;

@Service
public class AuthLogoutService {

    public void logout(HttpServletRequest request,
                       HttpServletResponse response,
                       Authentication authentication) {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        logoutHandler.logout(request, response, authentication);
        SecurityContextHolder.clearContext();

        expireCookie(response, "JSESSIONID", request.getContextPath());
        expireCookie(response, RememberMeConfig.REMEMBER_ME_PARAMETER, request.getContextPath());
        expireCookie(response, "JSESSIONID", "/");
        expireCookie(response, RememberMeConfig.REMEMBER_ME_PARAMETER, "/");
    }

    private void expireCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path == null || path.isBlank() ? "/" : path);
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }
}
