package com.hometalk.onepass.dashboard.entity.notification;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA 스펙 상 기본 생성자 필수, PROTECTED로 외부 직접 생성 차단함.
@AllArgsConstructor(access = AccessLevel.PRIVATE)       // @Builder 내부 동작용 전체 생성자, PRIVATE으로 외부 노출 차단함.
@Builder  // id를 제외하고 필요한 필드만 선택적으로 주입 가능함. 예: Book.builder().title("AI의 미래").price(30000).build()
@Entity
@Table(name = "AlarmNotification")  // 테이블명 명시. 생략 시 클래스명 기반 자동 지정됨. 테이블명은 복수형 사용.
public class NotificationCommon extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;         // 알림 발생 모듈

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;          // 등록한 회원 ID FK

    @Column(name = "category_alarm", nullable = false, length = 50)
    private String categoryAlarm;      // 모듈별 세부 분류

    @Column(nullable = false, length = 500)
    private String message;             // 알림 내용 메시지

//    @Column(name = "reference_id")
//    private Long referenceId;

    @Column(name = "is_read")
    private Boolean isRead;            // 읽음 여부 상태

    @Column(nullable = true)
    private LocalDateTime deletedAt;            // 삭제 시각
}
