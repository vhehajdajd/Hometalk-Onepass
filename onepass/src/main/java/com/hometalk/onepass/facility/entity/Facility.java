package com.hometalk.onepass.facility.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.*;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 무분별 객체 생성 방지용 !
@AllArgsConstructor
@Builder
@ToString
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // 기본키 (자동 증가)

    private String name;        // 시설 명

    private String description;     // 시설 설명

    private String category;      // 시설 종류

    @Column(nullable = false)
    private String location; // 단지 내 상세 위치 (예: 103동 지하 1층, 커뮤니티 센터 201호)

    private String address;  // (선택사항) 외부 주소가 필요한 경우

    private int capacity;       // 수용 인원

    @Embedded
    private OperationTime operationTime;        // OperationTime 파일 참조 오픈/마감 시간


}
