package com.hometalk.onepass.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /*
     * 공통 클라이언트 오류 (4xx)
     * 요청 자체가 잘못된 경우. 스프링이 자동으로 던지는 예외들을 핸들러에서
     * 잡아 아래 코드로 매핑해서 응답한다.
     */
    INVALID_INPUT_VALUE(400,     "C001", "잘못된 입력값입니다"),          // @Valid 검증 실패
    INVALID_TYPE_VALUE(400,      "C002", "타입이 올바르지 않습니다"),       // 파라미터 타입 불일치
    MISSING_PARAMETER(400,       "C003", "필수 파라미터가 없습니다"),       // 필수 쿼리 파라미터 누락
    UNREADABLE_REQUEST_BODY(400, "C004", "요청 본문을 읽을 수 없습니다"),   // JSON 파싱 오류
    CONSTRAINT_VIOLATION(400,    "C005", "제약 조건 위반입니다"),           // @Validated PathVariable 등

    /*
     * 인증 / 인가 오류
     * 로그인 여부(401)와 권한 여부(403)를 구분한다.
     */
    UNAUTHORIZED(401,            "A001", "인증이 필요합니다"),
    FORBIDDEN(403,               "A002", "접근 권한이 없습니다"),

    /*
     * 기타 클라이언트 오류
     */
    RESOURCE_NOT_FOUND(404,      "C006", "리소스를 찾을 수 없습니다"),      // 존재하지 않는 리소스
    METHOD_NOT_ALLOWED(405,      "C007", "지원하지 않는 HTTP 메서드입니다"),
    UNSUPPORTED_MEDIA_TYPE(415,  "C008", "지원하지 않는 미디어 타입입니다"),

    /*
     * 서버 오류 (5xx)
     * 예상하지 못한 예외가 터졌을 때 사용
     */
    INTERNAL_SERVER_ERROR(500,   "C999", "서버 내부 오류가 발생했습니다");

    private final int status;   // HTTP 상태 코드
    private final String code;  // 에러 코드
    private final String message; // 클라이언트에 노출되는 기본 메시지
}