package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Report;
import com.hometalk.onepass.community.enums.ReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 관리자 화면에서 신고한 목록을 User와 함께 조회
    @EntityGraph(attributePaths = {"user", "post"})
    List<Report> findByStatusOrderByIdDesc(ReportStatus status);

    boolean existsByPostIdAndUserId(Long postId, Long id);
}
