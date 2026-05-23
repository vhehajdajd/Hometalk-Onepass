package com.hometalk.onepass.complaint.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(2)
public class ComplaintExceptionHandler {

    // 1. 데이터 누락 (필수 입력값 체크)
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 400);
        map.put("message", ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return ResponseEntity.badRequest().body(map);
    }

    // 2. 비인가 접근 및 권한 부족 (401, 403 대응)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(ResponseStatusException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", ex.getStatusCode().value());
        map.put("message", ex.getReason());
        return new ResponseEntity<>(map, ex.getStatusCode());
    }

    // 3. 중복 민원 제기 (Conflict)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", 409);
        map.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(map);
    }
}