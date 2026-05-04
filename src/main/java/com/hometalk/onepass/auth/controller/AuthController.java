package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.auth.service.SignUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("")
    public String Auth() {
        return "auth/login";
    }


}