package com.hometalk.onepass.api.auth;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.dto.MyPageResponseDTO;
import com.hometalk.onepass.auth.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class MyPageApiController {

    private final MyPageService myPageService;

    @GetMapping("/mypage")
    public MyPageResponseDTO myPage(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return myPageService.getMyPage(userDetails);
    }
}
