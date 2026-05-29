package com.hometalk.onepass.api.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginApiController {

    @GetMapping("/login")
    public ResponseEntity<Map<String, String>> loginEndpointInfo() {
        // POST /api/auth/login is handled by Spring Security formLogin, not by this MVC controller.
        return ResponseEntity.ok(Map.of(
                "method", "POST",
                "contentType", "application/x-www-form-urlencoded",
                "loginId", "required",
                "password", "required"
        ));
    }
}
