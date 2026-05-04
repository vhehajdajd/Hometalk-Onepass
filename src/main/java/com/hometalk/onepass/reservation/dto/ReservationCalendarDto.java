package com.hometalk.onepass.reservation.dto;

import com.hometalk.onepass.reservation.entity.Reservation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ReservationCalendarDto {    private Long id;
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String badge;

    public static ReservationCalendarDto from(Reservation r) {
        ReservationCalendarDto dto = new ReservationCalendarDto();

        dto.setId(r.getId());
        dto.setTitle(
                r.getFacility().getName() + " (" +
                        r.getUser().getName() + ")"
        );

        dto.setStartAt(r.getReservationTime().getStartTime());
        dto.setEndAt(r.getReservationTime().getEndTime());
        dto.setBadge(r.getStatus().name());

        return dto;
    }
}
