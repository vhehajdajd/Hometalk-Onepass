package com.hometalk.onepass.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice // @ControllerAdvice + @ResponseBody. 모든 컨트롤러에 적용되는 전역 예외 핸들러.
public class GlobalExceptionHandler {
    // ── 1. @Valid 검증 실패 (RequestBody) ─────────────────────────────────────────
    /*
     * @RequestBody에 @Valid를 붙였을 때 검증 실패 시 스프링이 자동으로 던진다.
     * 어떤 필드가 왜 실패했는지 FieldError 목록으로 만들어서 응답에 포함한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e) {

        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(String.valueOf(fe.getRejectedValue()))
                        .reason(fe.getDefaultMessage()) // @NotBlank(message = "...") 에 적은 메시지
                        .build())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, fieldErrors));
    }

    // ── 2. @Validated 제약 조건 위반 (PathVariable, RequestParam) ─────────────────
    /*
     * 클래스 레벨에 @Validated를 붙이고 PathVariable / RequestParam에
     * @Min, @NotBlank 등을 적용했을 때 위반 시 발생한다.
     * (RequestBody의 @Valid와는 다른 예외 클래스)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException e) {
        log.warn("[ConstraintViolation] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.CONSTRAINT_VIOLATION));
    }

    // ── 3. 경로/쿼리 파라미터 타입 불일치 ────────────────────────────────────────
    /*
     * Long 타입의 변수에 String값이 입력된 경우
     * 어떤 파라미터가 문제인지 메시지에 포함해서 클라이언트가 디버깅하기 쉽게 한다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        log.warn("[TypeMismatch] param='{}', value='{}'", e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE,
                        String.format("'%s' 파라미터 타입이 올바르지 않습니다", e.getName())));
    }

    // ── 4. 필수 RequestParam 누락 ─────────────────────────────────────────────────
    /*
     * @RequestParam(required = true, default)인데 파라미터가 아예 없는 경우.
     * required = false 이거나 defaultValue가 있으면 이 예외가 발생하지 않는다.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException e) {
        log.warn("[MissingParam] '{}'", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.MISSING_PARAMETER,
                        String.format("'%s' 파라미터가 필요합니다", e.getParameterName())));
    }

    // ── 5. RequestBody 파싱 실패 ──────────────────────────────────────────────────
    /*
     * JSON 문법 오류, 잘못된 날짜 형식 등으로 RequestBody를 객체로 변환하지 못한 경우.
     * 원인 메시지에 내부 구현이 드러날 수 있으므로 클라이언트에는 단순 메시지만 반환한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e) {
        log.warn("[NotReadable] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.UNREADABLE_REQUEST_BODY));
    }

    // ── 6. 지원하지 않는 HTTP 메서드 ─────────────────────────────────────────────
    /*
     * ex) POST 전용 엔드포인트에 GET으로 요청하는 경우
     * 405 응답 시 어떤 메서드를 지원하는지 Allow 헤더에 자동으로 포함된다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotSupported] {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
    }

    // ── 7. 지원하지 않는 미디어 타입 ─────────────────────────────────────────────
    /*
     * API가 application/json을 기대하는데 multipart/form-data 등으로 요청한 경우.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    // ── 8. 이외 나머지 예외 ───────────────────────────────────────────────────────────
    /*
     * 위 핸들러가 처리하지 못한 모든 예외가 여기로 떨어진다.
     * 예상하지 못한 오류이므로 error 레벨로 전체를 기록한다.
     * 클라이언트에는 내부 구현이 드러나지 않도록 최소한의 메시지만 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[UnexpectedException]", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}