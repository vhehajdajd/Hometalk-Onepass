package com.hometalk.onepass.facility.dto;

import com.hometalk.onepass.facility.entity.BookingStatus;
import com.hometalk.onepass.facility.entity.Facility;
import com.hometalk.onepass.facility.entity.OperationTime;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FacilityResponseDto {
    private Long id;
    private String name;
    private String location;
    private String iconType;
    private String imagePath;
    private int maxCapacity;

    private BookingStatus bookingStatus;

    private String openTime;
    private String closeTime;

    private OperationTime operationTime;
    private Integer maxReservationTime;

    /*
        입주민 예약 화면용
     */
    public static FacilityResponseDto from(Facility entity, BookingStatus status) {
        return FacilityResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .location(entity.getLocation())
                .iconType(entity.getIconType())
                .imagePath(entity.getImagePath())
                .maxCapacity(entity.getMaxCapacity())
                .bookingStatus(status)
                .openTime(entity.getOperationTime() != null && entity.getOperationTime().getOpenTime() != null ? entity.getOperationTime().getOpenTime().toString() : null)
                .closeTime(entity.getOperationTime() != null && entity.getOperationTime().getCloseTime() != null ? entity.getOperationTime().getCloseTime().toString() : null)
                .operationTime(entity.getOperationTime())
                .maxReservationTime(entity.getMaxReservationTime())
                .build();
    }

    /*
        조회용
     */
    public static FacilityResponseDto from(Facility entity) {
        return FacilityResponseDto.from(entity, BookingStatus.AVAILABLE);
    }
}
