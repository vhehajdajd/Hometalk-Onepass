package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.service.DuplicationCheckService;
import com.hometalk.onepass.auth.util.LoginRedirectUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final DuplicationCheckService duplicationCheckService;

    @GetMapping("")
    public String Auth(@RequestParam(required = false) String redirectURL,
                       HttpServletRequest request) {
        LoginRedirectUtils.saveRedirectUrl(request, redirectURL);
        return "auth/login";
    }


    @GetMapping("/api/check-id-duplication")
    @ResponseBody
    public ResponseEntity<Boolean> checkId (@RequestParam("loginId") String loginId ) {
        return ResponseEntity.ok(duplicationCheckService.isIdDuplicated(loginId));
    }
}

