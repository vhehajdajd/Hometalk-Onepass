package com.hometalk.onepass.facility.entity;

// 시설 오픈/마감 시간

import jakarta.persistence.Embeddable;
import lombok.*;
import java.time.LocalTime;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OperationTime {

    private LocalTime openTime;
    private LocalTime closeTime;
}
