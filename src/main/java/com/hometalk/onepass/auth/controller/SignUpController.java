package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.dto.SocialSignUpDTO;
import com.hometalk.onepass.auth.config.CustomOAuth2User;
import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.service.EmailVerificationService;
import com.hometalk.onepass.auth.service.SignUpService;
import com.hometalk.onepass.auth.service.SocialSignUpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/auth/register")
public class SignUpController {

    private final SignUpService signUpService;
    private final SocialSignUpService socialSignUpService;
    private final EmailVerificationService emailVerificationService;

    @GetMapping("")
    public String Resister(Model model) {
        // step = 1로 초기값 설정
        model.addAttribute("step", 1);
        model.addAttribute("editMode", false);

        // 2. 타임리프 th:object와 연결될 빈 DTO 객체 전달
        model.addAttribute("signUpDTO", new SignUpDTO());

        return "auth/register";
    }

    @GetMapping("/edit")
    public String editRejectedRegistration(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           Model model) {
        if (userDetails == null) {
            return "redirect:/auth";
        }

        try {
            if (isSocialUser(userDetails)) {
                model.addAttribute("editMode", true);
                model.addAttribute("socialSignUpDTO", socialSignUpService.getRejectedSocialSignUpForm(userDetails.getUserId()));
                return "auth/register-social";
            }

            model.addAttribute("step", 1);
            model.addAttribute("editMode", true);
            model.addAttribute("signUpDTO", signUpService.getRejectedSignUpForm(userDetails.getUserId()));
            return "auth/register";
        } catch (IllegalArgumentException e) {
            return "redirect:/auth/approval/rejected";
        }
    }


    @PostMapping("/signup")   // 회원가입 단계별 목록 처리
    public String signup(
            @ModelAttribute("signUpDTO") SignUpDTO signUpDTO,      // DTO
            @RequestParam(required = false, defaultValue = "next") String action, // 버튼 상태
            @RequestParam(defaultValue = "1") int currentStep,  // 회원가입 단계
            HttpSession session,
            Model model
    ) {
        if ("next".equals(action)) {
            try {
                signUpService.validateLoginIdAvailable(signUpDTO.getLoginId());
                // signUpService.validateEmailAvailable(signUpDTO.getEmail());
                emailVerificationService.assertVerified(signUpDTO.getEmail(), session);
            } catch (IllegalArgumentException e) {
                model.addAttribute("step", 1);
                model.addAttribute("signupError", e.getMessage());
                return "auth/register";
            }

            model.addAttribute("step", currentStep + 1);
            return "auth/register"; // 본인의 html 파일명
        }

        if ("prev".equals(action)) {
            model.addAttribute("step", currentStep - 1);
            return "auth/register";
        }

        if ("complete".equals(action)) {
            try {
                emailVerificationService.assertVerified(signUpDTO.getEmail(), session);
                // 최종 서비스 로직 호출 (회원가입 처리)
                signUpService.signUp(signUpDTO);
                emailVerificationService.clear(session);
                return "redirect:/auth";
            } catch (IllegalArgumentException e) {
                model.addAttribute("step", 1);
                model.addAttribute("signupError", e.getMessage());
                return "auth/register";
            }
        }

        return "auth/register";
    }

    @PostMapping("/edit")
    public String updateRejectedRegistration(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @ModelAttribute("signUpDTO") SignUpDTO signUpDTO,
            @ModelAttribute("socialSignUpDTO") SocialSignUpDTO socialSignUpDTO,
            @RequestParam(required = false, defaultValue = "next") String action,
            @RequestParam(defaultValue = "1") int currentStep,
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        if (userDetails == null) {
            return "redirect:/auth";
        }

        if (isSocialUser(userDetails)) {
            try {
                User user = socialSignUpService.updateRejectedSocialSignUp(userDetails.getUserId(), socialSignUpDTO);
                replaceAuthenticationWithServiceUser(user, socialSignUpDTO, request);
                return "redirect:/auth/approval/pending";
            } catch (IllegalArgumentException e) {
                model.addAttribute("editMode", true);
                model.addAttribute("signupError", e.getMessage());
                return "auth/register-social";
            }
        }

        if ("next".equals(action)) {
            try {
                SignUpDTO currentDto = signUpService.getRejectedSignUpForm(userDetails.getUserId());
                signUpService.validateLoginIdAvailableForUser(signUpDTO.getLoginId(), userDetails.getUserId());
                if (!Objects.equals(currentDto.getEmail(), signUpDTO.getEmail())) {
                    emailVerificationService.assertVerified(signUpDTO.getEmail(), session);
                }
            } catch (IllegalArgumentException e) {
                model.addAttribute("step", 1);
                model.addAttribute("editMode", true);
                model.addAttribute("signupError", e.getMessage());
                return "auth/register";
            }

            model.addAttribute("step", currentStep + 1);
            model.addAttribute("editMode", true);
            return "auth/register";
        }

        if ("prev".equals(action)) {
            model.addAttribute("step", currentStep - 1);
            model.addAttribute("editMode", true);
            return "auth/register";
        }

        if ("complete".equals(action)) {
            try {
                SignUpDTO currentDto = signUpService.getRejectedSignUpForm(userDetails.getUserId());
                if (!Objects.equals(currentDto.getEmail(), signUpDTO.getEmail())) {
                    emailVerificationService.assertVerified(signUpDTO.getEmail(), session);
                }

                User user = signUpService.updateRejectedSignUp(userDetails.getUserId(), signUpDTO);
                emailVerificationService.clear(session);
                replaceAuthenticationWithLocalUser(user, request);
                return "redirect:/auth/approval/pending";
            } catch (IllegalArgumentException e) {
                model.addAttribute("step", 1);
                model.addAttribute("editMode", true);
                model.addAttribute("signupError", e.getMessage());
                return "auth/register";
            }
        }

        model.addAttribute("editMode", true);
        return "auth/register";
    }

    @PostMapping("/email/send")
    @ResponseBody
    public ResponseEntity<Map<String, String>> sendEmailVerificationCode(
            @RequestParam String email,
            HttpSession session
    ) {
        try {
            // signUpService.validateEmailAvailable(email);
            emailVerificationService.sendCode(email, session);
            return ResponseEntity.ok(Map.of("message", "인증 코드가 이메일로 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("이메일 인증 코드 발송 실패: email={}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/email/verify")
    @ResponseBody
    public ResponseEntity<Map<String, String>> verifyEmailCode(
            @RequestParam String email,
            @RequestParam String code,
            HttpSession session
    ) {
        try {
            emailVerificationService.verifyCode(email, code, session);
            return ResponseEntity.ok(Map.of("message", "이메일 인증이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
    public String registerSocialUser(@ModelAttribute("socialSignUpDTO") SocialSignUpDTO dto,
                                     HttpServletRequest request) {

        log.info("소셜 회원가입 시도: email={}, platform={}", dto.getEmail(), dto.getPlatform());

        // 서비스 메서드 호출
        User user = socialSignUpService.socialSignUp(dto);
        replaceAuthenticationWithServiceUser(user, dto, request);

        return "redirect:/auth/approval/pending";
    }

    private void replaceAuthenticationWithServiceUser(User user, SocialSignUpDTO dto, HttpServletRequest request) {
        OAuth2User currentOAuth2User = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof OAuth2User principal
                ? principal
                : null;

        Long householdId = user.getHousehold() != null ? user.getHousehold().getId() : null;
        String postNum = user.getHousehold() != null ? user.getHousehold().getPostNum() : null;
        String rememberMeUsername = "SOCIAL:" + dto.getPlatform().toUpperCase() + ":" + dto.getPlatformId();

        CustomOAuth2User principal = new CustomOAuth2User(
                user.getId(),
                householdId,
                postNum,
                user.getName(),
                user.getNickname(),
                user.getRole(),
                user.getStatus(),
                user.isApprovalNoticeShown(),
                rememberMeUsername,
                null,
                currentOAuth2User != null ? currentOAuth2User.getAttributes() : Map.of()
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void replaceAuthenticationWithLocalUser(User user, HttpServletRequest request) {
        Long householdId = user.getHousehold() != null ? user.getHousehold().getId() : null;
        String postNum = user.getHousehold() != null ? user.getHousehold().getPostNum() : null;

        CustomUserDetails principal = new CustomUserDetails(
                user.getId(),
                householdId,
                postNum,
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.isApprovalNoticeShown(),
                user.getLocalAccount().getLoginId(),
                user.getLocalAccount().getPasswordHash(),
                user.getNickname()
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private boolean isSocialUser(CustomUserDetails userDetails) {
        return userDetails instanceof CustomOAuth2User
                || (userDetails.getUsername() != null && userDetails.getUsername().startsWith("SOCIAL:"));
    }


}
