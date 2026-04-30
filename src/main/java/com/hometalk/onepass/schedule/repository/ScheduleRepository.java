package com.hometalk.onepass.schedule.repository;

import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // 특정 기간 내 일정 조회 (달력용)
    List<Schedule> findByStartAtBetween(LocalDateTime start, LocalDateTime end);

    // 공지로 연결된 일정 조회
    Optional<Schedule> findFirstByNotice(Notice notice);
}