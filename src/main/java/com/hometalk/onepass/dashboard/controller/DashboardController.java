package com.hometalk.onepass.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    /* ── 로그인 후 역할별 분기 ── */
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/";
        }

        String role = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return switch (role) {
            case "ROLE_ADMIN"    -> "dashboard/admin";
            case "ROLE_RESIDENT" -> "dashboard/resident";
            case "ROLE_STAFF"    -> "redirect:/staff/entry";
            case "ROLE_MEMBER"   -> "redirect:/home";
            default              -> "redirect:/";
        };
    }
}
