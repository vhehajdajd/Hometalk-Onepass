package com.hometalk.onepass.facility.dto;

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

    private String openTime;
    private String closeTime;

    private OperationTime operationTime;
    private Integer maxReservationTime;

    public static FacilityResponseDto from(Facility entity) {
        return FacilityResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .location(entity.getLocation())
                .iconType(entity.getIconType())
                .imagePath(entity.getImagePath())
                .maxCapacity(entity.getMaxCapacity())
                .openTime(entity.getOperationTime().getOpenTime().toString())
                .closeTime(entity.getOperationTime().getCloseTime().toString())
                .operationTime(entity.getOperationTime())
                .maxReservationTime(entity.getMaxReservationTime())
                .build();
    }
}
