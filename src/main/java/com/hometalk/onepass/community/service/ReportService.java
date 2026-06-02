package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.ReportRequestDTO;
import com.hometalk.onepass.community.dto.ReportResponse;
import com.hometalk.onepass.community.dto.ReportSummaryDTO;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.entity.Report;
import com.hometalk.onepass.community.enums.ReportReason;
import com.hometalk.onepass.community.enums.ReportStatus;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.exception.UserNotFoundException;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /*
        [유저용] 커뮤니티 게시글 신고 접수
          - 초기 상태: PENDING (대기)
     */
    public void createReport(ReportRequestDTO dto, Long userId) {
        if (userId == null) {
            throw new UserNotFoundException(null, "로그인이 필요한 서비스입니다.");
        }

        // 중복 신고 검증
        boolean alreadyReported = reportRepository.existsByPostIdAndUserId(dto.getPostId(), userId);
        if (alreadyReported) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }

        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new PostNotFoundException(dto.getPostId(), "존재하지 않는 게시글입니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId, "존재하지 않는 회원입니다."));
        if (post.isPinned()) {
            throw new IllegalStateException("관리자가 고정한 게시글은 신고할 수 없습니다.");
        }

        // 빌더로 조립 후 저장
        Report report = Report.builder()
                .reason(dto.getReason())
                .detail(dto.getDetail())
                .post(post)
                .user(user)
                .build();

        reportRepository.save(report);
    }

    /*
        [관리자용] 신고 상태 필터링
     */
    @Transactional(readOnly = true)
    public List<ReportSummaryDTO> getReportSummaryByFilters(String statusStr, String reasonStr) {
        ReportStatus status = null;
        ReportReason reason = null;

        // 1. 상태값 변환 및 안전 검증
        if (statusStr != null && !statusStr.isBlank() && !"ALL".equalsIgnoreCase(statusStr)) {
            try {
                status = ReportStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        // 2. 신고 유형값 변환 및 안전 검증
        if (reasonStr != null && !reasonStr.isBlank()) {
            try {
                reason = ReportReason.valueOf(reasonStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return reportRepository.getReportSummaryByFilters(status, reason);
    }

    // 해당 게시글의 모든 신고 내역 조회
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByPostId(Long postId) {
        return reportRepository.findByPostIdOrderByIdDesc(postId).stream()
                .map(ReportResponse::from)
                .collect(Collectors.toList());
    }

    /*
        관리자 상세 내역 조회 시 '검토중' 상태 변경
     */
    @Transactional
    public void markReviewing(Long postId) {
        List<Report> reports = reportRepository.findByPostIdOrderByIdDesc(postId);
        if (reports.isEmpty()) return;

        reports.stream()
                .filter(report -> report.getStatus() == ReportStatus.PENDING)
                .forEach(report -> report.updateStatus(ReportStatus.REVIEWING));
    }

    /*
        [관리자용] 신고 처리 완료 (RESOLVED)
         - 신고 승인되면 해당 게시글 삭제 처리
     */
    public void resolveReport(Long postId) {
        List<Report> reports = reportRepository.findByPostIdOrderByIdDesc(postId);
        if (reports.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 신고 게시글입니다: " + postId);
        }

        reports.forEach(report -> report.updateStatus(ReportStatus.RESOLVED));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId, "존재하지 않는 게시글입니다."));

        post.softDelete();
    }

    /*
        [관리자용] 신고 반려 (REJECTED)
         - 허위 신고이거나 문제가 없다고 판단하여 매칭 취소
     */
    public void rejectReport(Long postId) {
        List<Report> reports = reportRepository.findByPostIdOrderByIdDesc(postId);

        if (reports.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 신고 게시글입니다: " + postId);
        }

        reports.forEach(report -> report.updateStatus(ReportStatus.REJECTED));
    }
}