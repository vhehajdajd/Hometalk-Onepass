package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.dto.SocialSignUpDTO;
import com.hometalk.onepass.auth.service.SignUpService;
import com.hometalk.onepass.auth.service.SocialSignUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/register")
public class SignUpController {

    private final SignUpService signUpService;
    private final SocialSignUpService socialSignUpService;

    @GetMapping("")
    public String Resister(Model model) {
        // step = 1로 초기값 설정
        model.addAttribute("step", 1);

        // 2. 타임리프 th:object와 연결될 빈 DTO 객체 전달
        model.addAttribute("signUpDTO", new SignUpDTO());

        return "auth/register";
    }


    @PostMapping("/signup")   // 회원가입 단계별 목록 처리
    public String signup(
            @ModelAttribute("signUpDTO") SignUpDTO signUpDTO,      // DTO
            @RequestParam(required = false, defaultValue = "next") String action, // 버튼 상태
            @RequestParam(defaultValue = "1") int currentStep,  // 회원가입 단계
            Model model
    ) {
        if ("next".equals(action)) {
            model.addAttribute("step", currentStep + 1);
            return "auth/register"; // 본인의 html 파일명
        }

        if ("prev".equals(action)) {
            model.addAttribute("step", currentStep - 1);
            return "auth/register";
        }

        if ("complete".equals(action)) {
            // 최종 서비스 로직 호출 (회원가입 처리)
            signUpService.signUp(signUpDTO);
            return "redirect:/auth";
        }

        return "auth/register";
    }










    /**
     * 소셜 로그인 추가 정보 입력 폼
     */
    @GetMapping("/social")
    public String socialSignupForm(@RequestParam String email,
                                   @RequestParam String platform,
                                   @RequestParam String platformId,
                                   @RequestParam(required = false) String nickname, // 소셜에서 준 닉네임
                                   Model model) {

        // DTO를 미리 생성해서 모델에 담아주면 타임리프 th:field 사용이 가능해집니다.
        SocialSignUpDTO socialSignUpDTO = new SocialSignUpDTO();
        socialSignUpDTO.setEmail(email);
        socialSignUpDTO.setPlatform(platform);
        socialSignUpDTO.setPlatformId(platformId);
        socialSignUpDTO.setNickname(nickname); // 소셜 닉네임을 기본값으로 세팅

        model.addAttribute("socialSignUpDTO", socialSignUpDTO);

        return "auth/register-social";
    }

    /**
     * 소셜 가입 완료 처리
     */
    @PostMapping("/social")
    public String registerSocialUser(@ModelAttribute("socialSignUpDTO") SocialSignUpDTO dto) {

        log.info("소셜 회원가입 시도: email={}, platform={}", dto.getEmail(), dto.getPlatform());

        // 서비스 메서드 호출
        socialSignUpService.socialSignUp(dto);

        return "redirect:/index";
    }


}
