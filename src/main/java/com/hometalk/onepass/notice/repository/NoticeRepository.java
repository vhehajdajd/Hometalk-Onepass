package com.hometalk.onepass.notice.repository;

import com.hometalk.onepass.notice.entity.Badge;
import com.hometalk.onepass.notice.entity.Notice;
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
}