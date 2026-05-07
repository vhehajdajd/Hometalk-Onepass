package com.hometalk.onepass.billing.controller;

import com.hometalk.onepass.billing.scheduler.BillingScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ⚠️ 개발/테스트 전용 컨트롤러 — 운영 배포 시 반드시 삭제
 *
 * BillingScheduler 수동 트리거용
 * @Profile("dev") — application.properties에 spring.profiles.active=dev 설정 시에만 활성화
 */
@Profile("dev")
@RestController
@RequestMapping("/api/test/scheduler")
@RequiredArgsConstructor
public class BillingSchedulerTestController {

    private final BillingScheduler billingScheduler;

    /**
     * 미납/체납 감지 스케줄러 수동 실행
     * POST /api/test/scheduler/unpaid
     */
    @PostMapping("/unpaid")
    public ResponseEntity<Map<String, String>> triggerUnpaid() {
        billingScheduler.detectUnpaidAndNotify();
        return ResponseEntity.ok(Map.of("result", "detectUnpaidAndNotify 실행 완료"));
    }

    /**
     * 전월 정산 요약 스케줄러 수동 실행
     * POST /api/test/scheduler/monthly-summary
     */
    @PostMapping("/monthly-summary")
    public ResponseEntity<Map<String, String>> triggerMonthlySummary() {
        billingScheduler.sendMonthlySummary();
        return ResponseEntity.ok(Map.of("result", "sendMonthlySummary 실행 완료"));
    }
}