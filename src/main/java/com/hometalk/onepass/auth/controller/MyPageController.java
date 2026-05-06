package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.dto.MyPageResponseDTO;
import com.hometalk.onepass.auth.service.MyPageService;
import com.hometalk.onepass.auth.service.WithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final WithdrawalService withdrawalService;

    @GetMapping("/myPage")
    public String myPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        // principal에 담긴 사용자 식별자를 기준으로 마이페이지 정보를 조회한다.
        MyPageResponseDTO myPage = myPageService.getMyPage(userDetails);
        model.addAttribute("myPage", myPage);
        return "auth/my-page";
    }

    @PostMapping("/auth/withdraw")
    public String withdraw(Authentication authentication,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        // 탈퇴 직후 동일 세션이 남지 않도록 보안 컨텍스트까지 함께 정리한다.
        withdrawalService.withdraw(authentication);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/auth?withdrawn=true";
    }
}
