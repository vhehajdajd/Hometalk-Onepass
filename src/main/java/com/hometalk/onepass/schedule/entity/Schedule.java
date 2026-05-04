package com.hometalk.onepass.schedule.entity;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.common.entity.BaseTimeEntity;
import com.hometalk.onepass.notice.entity.Badge;
import com.hometalk.onepass.notice.entity.Notice;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Column(nullable = false, length = 100)
    private String title;

    private String info;
    private String location;
    private String referenceUrl;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column
    private LocalDateTime endAt;

    // 공지 없는 독립 일정의 배지 (공지 연동 시 공지 배지 우선)
    @Enumerated(EnumType.STRING)
    @Column(name = "badge", columnDefinition = "VARCHAR(20)")
    private Badge badge;

    @Enumerated(EnumType.STRING)
    @Column(name = "repeat_type", columnDefinition = "VARCHAR(20)")
    private RepeatType repeatType;

    private LocalDateTime repeatEndAt;

    private Long repeatGroupId;

    @Builder
    public Schedule(User user, Notice notice, String title, String info,
                    String location, String referenceUrl,
                    LocalDateTime startAt, LocalDateTime endAt, Badge badge,
                    RepeatType repeatType, LocalDateTime repeatEndAt, Long repeatGroupId) {
        validateTitle(title);
        validateTime(startAt, endAt);
        this.user = user;
        this.notice = notice;
        this.title = title;
        this.info = info;
        this.location = location;
        this.referenceUrl = referenceUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.badge = badge;
        this.repeatType = repeatType;
        this.repeatEndAt = repeatEndAt;
        this.repeatGroupId = repeatGroupId;
    }

    //수정메서드
    public void update(String title, String info, String location,
                       String referenceUrl, LocalDateTime startAt, LocalDateTime endAt,
                       Badge badge, RepeatType repeatType, LocalDateTime repeatEndAt) {
        validateTitle(title);
        validateTime(startAt, endAt);
        this.title = title;
        this.info = info;
        this.location = location;
        this.referenceUrl = referenceUrl;
        this.startAt = startAt;
        this.endAt = endAt;
        this.badge = badge;
        this.repeatType = repeatType;
        this.repeatEndAt = repeatEndAt;
    }

    // 실제 달력에 표시할 배지 반환 (공지 배지 우선)
    public Badge getEffectiveBadge() {
        if (this.notice != null && this.notice.getBadge() != null) {
            return this.notice.getBadge();
        }
        return this.badge;
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
    }

    private void validateTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null) {
            throw new IllegalArgumentException("시작 시간을 입력해주세요.");
        }
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("종료 시간이 시작 시간보다 빠를 수 없습니다.");
        }
    }
}