package com.hometalk.onepass.facility.entity;

import jakarta.persistence.*;
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

    @Embedded
    private OperationTime operationTime; // LocalTime 기반 운영 시간


    public void updateInfo(String name, String location, String iconType, String imagePath, OperationTime operationTime) {
        this.name = name;
        this.location = location;
        this.iconType = iconType;
        this.imagePath = imagePath;
        this.operationTime = operationTime;
    }
}