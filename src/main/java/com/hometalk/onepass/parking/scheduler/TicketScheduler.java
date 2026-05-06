package com.hometalk.onepass.parking.scheduler;

import com.hometalk.onepass.parking.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketScheduler {

    private final TicketService ticketService;

    // 매달 1일 00:00 자동 발급
    // 멀티 서버 환경에서는 중복 실행 가능
    // → UniqueConstraint + DataIntegrityViolationException 처리로 방어
    @Scheduled(cron = "0 0 0 1 * *")
    public void issueMonthlyTickets() {
        log.info("월별 티켓 자동 발급 시작");
        ticketService.issueMonthlyTickets();
        log.info("월별 티켓 자동 발급 완료");
    }
}