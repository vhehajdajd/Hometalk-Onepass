package com.hometalk.onepass.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualEntryRequest {

    @NotBlank
    private String purposeType; // 택배 | 방문 | 기타

    private String dong; // 동호수 모를 시 null 허용

    private String ho;   // 동호수 모를 시 null 허용

    @NotBlank
    private String vehicleNumber;
}