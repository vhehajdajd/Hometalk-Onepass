package com.hometalk.onepass.reservation.dto;

import com.hometalk.onepass.reservation.entity.Reservation;
import com.hometalk.onepass.reservation.entity.ReservationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class ReservationResponseDto {
    private Long id;
    private String facilityName;
    private String userName;      // 💡 관리자 페이지의 핵심 데이터
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime actualEndTime;
    private ReservationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReservationResponseDto fromEntity(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setId(reservation.getId());
        dto.setFacilityName(reservation.getFacility().getName());

        // 1. 예약자 이름 세팅 (User 엔티티와의 연관관계 활용)
        if (reservation.getUser() != null) {
            dto.setUserName(reservation.getUser().getName());
        } else {
            dto.setUserName("알 수 없는 사용자");
        }

        // 2. 예약 시간 세팅
        if (reservation.getReservationTime() != null) {
            dto.setStartTime(reservation.getReservationTime().getStartTime());
            dto.setEndTime(reservation.getReservationTime().getEndTime());
        }

        dto.setActualEndTime(reservation.getActualEndTime());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt());
        dto.setUpdatedAt(reservation.getUpdatedAt());
        return dto;
    }
}