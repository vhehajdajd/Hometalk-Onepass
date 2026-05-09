package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.util.LoginRedirectUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginRedirectController {

    // home.html의 미로그인 이동 경로가 /login이므로,
    // 실제 Spring Security 로그인 페이지인 /auth로 연결한다.
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String redirectURL,
                        HttpServletRequest request) {
        LoginRedirectUtils.saveRedirectUrl(request, redirectURL);
        return "redirect:/auth";
    }
}
