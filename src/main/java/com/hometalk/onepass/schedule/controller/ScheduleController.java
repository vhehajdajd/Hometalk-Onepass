package com.hometalk.onepass.schedule.controller;

import com.hometalk.onepass.schedule.dto.ScheduleCalResponseDto;
import com.hometalk.onepass.schedule.dto.ScheduleDetailResponseDto;
import com.hometalk.onepass.schedule.dto.ScheduleRequestDto;
import com.hometalk.onepass.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    // ── 달력 페이지 ───────────────────────────────────────────────────────────
    @GetMapping
    public String schedulePage() {
        return "schedule/scheduleCalendar";
    }

    // ── 달력용 일정 목록 조회 (JSON) ──────────────────────────────────────────
    // 달력 JS에서 Ajax로 호출
    @GetMapping("/api/list")
    @ResponseBody
    public List<ScheduleCalResponseDto> getSchedulesByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        return scheduleService.getSchedulesByMonth(year, month);
    }

    // ── 일정 상세 조회 (JSON) ─────────────────────────────────────────────────
    // 달력에서 일정 클릭 시 모달에 표시
    @GetMapping("/api/{id}")
    @ResponseBody
    public ScheduleDetailResponseDto getScheduleDetail(@PathVariable Long id) {
        return scheduleService.getScheduleDetail(id);
    }

    // ── 일정 등록 (JSON) ──────────────────────────────────────────────────────
    // 일정 페이지에서 독립적으로 등록
    @PostMapping("/write")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createSchedule(@RequestBody ScheduleRequestDto dto) {
        Long id = scheduleService.createSchedule(dto);
        return ResponseEntity.ok(id);
    }

    // ── 일정 수정 (JSON) ──────────────────────────────────────────────────────
    @PostMapping("/api/{id}/edit")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> updateSchedule(@PathVariable Long id,
                                               @RequestBody ScheduleRequestDto dto) {
        Long updatedId = scheduleService.updateSchedule(id, dto);
        return ResponseEntity.ok(updatedId);
    }

    // ── 일정 삭제 (JSON) ──────────────────────────────────────────────────────
    @DeleteMapping("/api/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.ok().build();
    }
}