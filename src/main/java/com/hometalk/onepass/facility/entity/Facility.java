package com.hometalk.onepass.facility.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "kjh_facility")
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    private String location;

    private String iconType;    // 디자인 가이드 아이콘
    private String imagePath;   // 시설 전경 사진
    private int maxCapacity;    // 시설별 최대 인원

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BookingStatus status;

    @Embedded
    private OperationTime operationTime; // LocalTime 기반 운영 시간

    @NotNull(message = "최대 예약 가능 시간은 필수 입력 항목입니다.")
    @Min(value = 1, message = "최대 예약 시간은 최소 1시간 이상이어야 합니다.")
    @Max(value = 24, message = "최대 예약 시간은 24시간을 초과할 수 없습니다.")
    private Integer maxReservationTime;


    public void updateInfo(String name, String location, String iconType, String imagePath, int maxCapacity,
                           OperationTime operationTime, Integer maxReservationTime) {
        this.name = name;
        this.location = location;
        this.iconType = iconType;
        this.imagePath = imagePath;
        this.maxCapacity = maxCapacity;
        this.operationTime = operationTime;
        this.maxReservationTime = maxReservationTime;
    }
}