package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.ReportRequestDTO;
import com.hometalk.onepass.community.dto.ReportResponse;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.entity.Report;
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
        [관리자용] 대기(PENDING) 상태의 신고 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReportResponse> findPendingReports() {
        return reportRepository.findByStatusOrderByIdDesc(ReportStatus.PENDING)
                .stream()
                .map(ReportResponse::from)
                .collect(Collectors.toList());
    }

    /*
        [관리자용] 신고 처리 완료 (RESOLVED)
         - 신고 승인되면 해당 게시글 삭제 처리
     */
    public void resolveReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고 내역입니다: " + reportId));

        report.updateStatus(ReportStatus.RESOLVED);
        Post post = report.getPost();
        if (post != null) {
            post.softDelete();
        }
    }

    /*
        [관리자용] 신고 반려 (REJECTED)
         - 허위 신고이거나 문제가 없다고 판단하여 매칭 취소
     */
    public void rejectReport(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신고 내역입니다: " + reportId));

        report.updateStatus(ReportStatus.REJECTED);
    }
}
