package com.hometalk.onepass.reservation.exception;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Order(3)

public class ReservationExceptionHandler {


        // 예약 불가 상태 (이미 예약된 시간 등): 409 Conflict
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<Map<String, Object>> handleReservationConflict(IllegalStateException ex) {
            Map<String, Object> map = new HashMap<>();
            map.put("status", 409);
            map.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(map);
        }

        // 잘못된 예약 요청 (과거 날짜 예약 등): 400 Bad Request
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
            Map<String, Object> map = new HashMap<>();
            map.put("status", 400);
            map.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(map);
        }
    }


