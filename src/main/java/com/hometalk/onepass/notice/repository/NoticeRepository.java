package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.entity.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 이전글, 다음글
    Optional<Notice> findFirstByIdLessThanOrderByIdDesc(Long id);
    Optional<Notice> findFirstByIdGreaterThanOrderByIdAsc(Long id);

    // 상단 고정
    List<Notice> findAllByOrderByIsPinnedDescCreatedAtDesc();

    // 제목+내용 검색
    Page<Notice> findByTitleContainingOrContentContaining(String title, String content, Pageable pageable);

    // 제목만 검색
    Page<Notice> findByTitleContaining(String title, Pageable pageable);

    // PUBLISHED인 것만 목록 조회
    Page<Notice> findByStatus(NoticeStatus status, Pageable pageable);

    // PUBLISHED인 것만 제목 검색
    Page<Notice> findByStatusAndTitleContaining(NoticeStatus status, String title, Pageable pageable);

    // PUBLISHED인 것만 제목+내용 검색
    Page<Notice> findByStatusAndTitleContainingOrStatusAndContentContaining(
            NoticeStatus status1, String title, NoticeStatus status2, String content, Pageable pageable);

    // 내 임시저장 목록
    List<Notice> findByUserAndStatusOrderByCreatedAtDesc(User user, NoticeStatus status);

    List<Notice> findTop5ByStatusOrderByCreatedAtDesc(NoticeStatus status);

}