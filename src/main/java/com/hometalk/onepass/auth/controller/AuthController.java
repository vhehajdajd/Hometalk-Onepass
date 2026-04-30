package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.auth.service.DuplicationCheckService;
import com.hometalk.onepass.auth.service.SignUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {

    private final DuplicationCheckService duplicationCheckService;

    @GetMapping("")
    public String Auth() {
        return "auth/login";
    }


    @GetMapping("/api/check-id-duplication")
    @ResponseBody
    public ResponseEntity<Boolean> checkId (@RequestParam("loginId") String loginId ) {
        return ResponseEntity.ok(duplicationCheckService.isIdDuplicated(loginId));
    }
}

