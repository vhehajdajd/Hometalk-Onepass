package com.hometalk.onepass.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    /*
     * 모든 에러 응답의 공통 포맷.
     * 성공 응답과 구조를 통일해서 클라이언트가 일관되게 파싱할 수 있도록 한다.
     *
     * 응답 예시 (단순 오류):
     * {
     *   "status": 404,
     *   "code": "C006",
     *   "message": "리소스를 찾을 수 없습니다",
     *   "errors": []
     * }
     *
     * 응답 예시 (@Valid 실패 - 필드별 상세 포함):
     * {
     *   "status": 400,
     *   "code": "C001",
     *   "message": "잘못된 입력값입니다",
     *   "errors": [
     *     { "field": "email", "rejectedValue": "abc", "reason": "이메일 형식이 아닙니다" },
     *     { "field": "password", "rejectedValue": "", "reason": "비밀번호는 필수입니다" }
     *   ]
     * }
     */

    private final int status;
    private final String code;
    private final String message;

    @Builder.Default
    private final List<FieldError> errors = Collections.emptyList(); // 필드 오류 없으면 빈 배열 반환

    // 단순 오류 (필드 에러 없음)
    public static ErrorResponse of(ErrorCode ec) {
        return ErrorResponse.builder()
                .status(ec.getStatus())
                .code(ec.getCode())
                .message(ec.getMessage())
                .build();
    }

    // 에러 코드 + 메시지 오버라이드 (BusinessException 상세 메시지 전달 시)
    public static ErrorResponse of(ErrorCode ec, String message) {
        return ErrorResponse.builder()
                .status(ec.getStatus())
                .code(ec.getCode())
                .message(message)
                .build();
    }

    // 에러 코드 + 필드별 상세 오류 목록 (@Valid 실패 시)
    public static ErrorResponse of(ErrorCode ec, List<FieldError> errors) {
        return ErrorResponse.builder()
                .status(ec.getStatus())
                .code(ec.getCode())
                .message(ec.getMessage())
                .errors(errors)
                .build();
    }

    /*
     * @Valid 검증 실패 시 어떤 필드가 왜 실패했는지 담는 내부 클래스.
     * 필드 오류가 없는 경우엔 errors 배열 자체가 빈 배열로 응답된다.
     */
    @Getter
    @Builder
    public static class FieldError {
        private final String field;          // 오류가 발생한 필드명 (예: "email")
        private final String rejectedValue;  // 실제로 들어온 잘못된 값
        private final String reason;         // 왜 실패했는지 (예: "이메일 형식이 아닙니다")
    }
}