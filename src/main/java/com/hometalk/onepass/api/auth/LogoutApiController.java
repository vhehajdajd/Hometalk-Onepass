package com.hometalk.onepass.api.auth;

import com.hometalk.onepass.auth.service.AuthLogoutService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LogoutApiController {

    private final AuthLogoutService authLogoutService;

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                      HttpServletResponse response,
                                                      Authentication authentication) {
        authLogoutService.logout(request, response, authentication);
        return ResponseEntity.ok(Map.of("redirectUrl", "/auth?logout=true"));
    }
}
