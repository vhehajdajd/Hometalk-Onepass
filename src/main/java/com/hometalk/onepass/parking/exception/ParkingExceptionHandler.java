package com.hometalk.onepass.parking.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.hometalk.onepass.parking")
public class ParkingExceptionHandler {

    // 비즈니스 로직 예외 (이미 입차, 티켓 부족, 이미 출차 등)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", e.getMessage()));
    }

    // 잘못된 요청 예외 (차량 정보 없음, 예약 정보 없음, 세대 없음 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(Map.of("message", e.getMessage()));
    }

    // 엔티티 조회 실패 (주차 기록 없음, 세대 없음 등)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException e) {
        return ResponseEntity
                .status(404)
                .body(Map.of("message", e.getMessage()));
    }
}