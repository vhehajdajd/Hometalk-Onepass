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

    // 같은 반복 그룹 조회
    List<Schedule> findByRepeatGroupId(Long repeatGroupId);

    // 같은 반복 그룹에서 특정 날짜 이후 조회
    List<Schedule> findByRepeatGroupIdAndStartAtGreaterThanEqual(Long repeatGroupId, LocalDateTime startAt);

    // 공지로 연결된 일정 전체 조회 (반복일정 삭제용)
    List<Schedule> findByNotice(Notice notice);

}