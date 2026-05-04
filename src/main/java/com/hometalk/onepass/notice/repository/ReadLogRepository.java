package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.entity.ReadLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReadLogRepository extends JpaRepository<ReadLog, Long> {

    boolean existsByUserAndNotice(User user, Notice notice);

    // 7일 이내 공지 중 안 읽은 것
    @Query("SELECT n FROM Notice n WHERE n.createdAt >= :since AND n.status = 'PUBLISHED' " +
            "AND n NOT IN (SELECT r.notice FROM ReadLog r WHERE r.user = :user)")
    List<Notice> findUnreadRecentNotices(@Param("user") User user,
                                         @Param("since") LocalDateTime since);

    void deleteByNotice(Notice notice);
}