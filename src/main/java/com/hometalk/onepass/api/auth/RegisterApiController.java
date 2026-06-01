package com.hometalk.onepass.api.auth;

import com.hometalk.onepass.auth.dto.SignUpDTO;
import com.hometalk.onepass.auth.service.SignUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class RegisterApiController {

    // 기존 Thymeleaf 회원가입 흐름에서도 사용하는 서비스다.
    // API 컨트롤러도 같은 서비스를 재사용해 비밀번호 암호화, 세대 생성, 계정 생성 로직을 한 곳에 둔다.
    private final SignUpService signUpService;

    // React 회원가입 화면에서 POST /hometop/api/auth/register로 호출하는 엔드포인트다.
    // 컨트롤러 매핑은 Spring context-path(/hometop)를 제외한 /api/auth/register 기준으로 선언한다.
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody SignUpDTO request) {
        // @RequestBody는 JSON body를 SignUpDTO로 변환하고, @Valid는 DTO의 필수값 검증을 수행한다.
        // 실제 저장 처리는 서비스에 위임한다.
        signUpService.signUp(request);
        // 정상 저장되면 201 Created와 메시지를 반환한다.
        // 프론트는 response.ok를 확인한 뒤 로그인 화면으로 이동한다.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "회원가입 요청이 완료되었습니다."));
    }

    // SignUpService에서 아이디 중복, 빈 아이디 같은 비즈니스 검증 실패 시 IllegalArgumentException을 던진다.
    // 이를 400 Bad Request와 JSON message로 변환해 React 화면에서 사용자에게 표시할 수 있게 한다.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage()));
    }
}
