package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.dto.ReportSummaryDTO;
import com.hometalk.onepass.community.entity.Report;
import com.hometalk.onepass.community.enums.ReportReason;
import com.hometalk.onepass.community.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @Query("""
            SELECT new com.hometalk.onepass.community.dto.ReportSummaryDTO(
                r.post.id,
                r.post.title,
                r.post.board.code,
                r.post.category.code,
                COUNT(r),
                MIN(r.id),
                (
                    SELECT r2.status
                    FROM Report r2
                    WHERE r2.id = MIN(r.id)
                )
            )
            FROM Report r
            WHERE (
                (:status IS NULL AND r.status IN (
                    com.hometalk.onepass.community.enums.ReportStatus.PENDING,
                    com.hometalk.onepass.community.enums.ReportStatus.REVIEWING
                ))
                OR (:status IS NOT NULL AND r.status = :status)
            )
              AND (:reason IS NULL OR r.reason = :reason)
            GROUP BY r.post.id, r.post.title, r.post.board.code, r.post.category.code
            ORDER BY MIN(r.id) DESC
        """)
    List<ReportSummaryDTO> getReportSummaryByFilters(
            @Param("status") ReportStatus status,
            @Param("reason") ReportReason reason
    );

    boolean existsByPostIdAndUserId(Long postId, Long id);

    List<Report> findByPostIdOrderByIdDesc(Long postId);
}
