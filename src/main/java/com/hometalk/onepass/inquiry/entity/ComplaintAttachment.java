package com.hometalk.onepass.inquiry.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter // ← 이거 하나면 getId, getOriginFileName 등이 자동으로 생겨요!
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder // ← 지현님이 직접 짜셨던 복잡한 빌더 코드를 롬복이 한 줄로 대신합니다.
@Table(name = "kjh_complaint_attachment")
public class ComplaintAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String originFileName;
    private String storedFileName; // DTO와 이름 맞춤
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;
}