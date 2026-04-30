package com.hometalk.onepass.dashboard.repository.notification;

import com.hometalk.onepass.dashboard.entity.notification.NotificationCommon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 *   도서관련 DB 접근을 담당하는 Repository
 *   Book 엔티티, PK 타입 Long
 *   - 기본 제공 메서드 : save() -> INSERT / UPDATE, findeAll() -> SELECT, findByid() -> SELECT by PK, delete() 등
 *   - 커스텀 쿼리가 필요하면 @Query 추가, 명명 규칙 등
 * */
public interface NotificationRepository extends JpaRepository<NotificationCommon, Long> {

    // boolean 값(status)을 인자로 받아 검색
    List<NotificationCommon> findByIsReadOrderByCreatedAtDesc(Boolean status);
}