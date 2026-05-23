package com.hometalk.onepass.inquiry.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.annotation.Order;

import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
@Order(1)
public class ServiceExceptionHandler {

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 400);
        // 첫 번째 에러 메시지만 가져와서 사용자에게 보여줌
        map.put("message", ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return ResponseEntity.badRequest().body(map);
    }

    // 2. 비인가 접근 및 권한 부족 (401, 403 대응)
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(org.springframework.web.server.ResponseStatusException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", ex.getStatusCode().value());
        map.put("message", ex.getReason());
        return new ResponseEntity<>(map, ex.getStatusCode());
    }

    // 3.  중복 제출
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 409); // Conflict
        map.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(map);
    }
}