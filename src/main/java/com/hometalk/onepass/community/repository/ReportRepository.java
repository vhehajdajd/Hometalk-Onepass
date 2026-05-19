package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Report;
import com.hometalk.onepass.community.enums.ReportReason;
import com.hometalk.onepass.community.enums.ReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    // 관리자 화면에서 신고한 목록을 User와 함께 조회
    @EntityGraph(attributePaths = {"user", "post", "post.board", "post.category"})
    List<Report> findByStatusOrderByIdDesc(ReportStatus status);

    // 전체 조회 최적화
    @EntityGraph(attributePaths = {"user", "post", "post.board", "post.category"})
    List<Report> findAllByOrderByIdDesc();

    // 신고 유형 & 상태 조회
    @EntityGraph(attributePaths = {"user", "post", "post.board", "post.category"})
    List<Report> findByStatusAndReasonOrderByIdDesc(ReportStatus status, ReportReason reason);

    // 신고 유형 데이터 최신순 조회
    @EntityGraph(attributePaths = {"user", "post", "post.board", "post.category"})
    List<Report> findByReasonOrderByIdDesc(ReportReason reason);

    boolean existsByPostIdAndUserId(Long postId, Long id);
}
