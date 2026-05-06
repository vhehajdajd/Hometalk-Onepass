package com.hometalk.onepass.complaint.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.complaint.config.FileProperties;
import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.entity.Complaint;
import com.hometalk.onepass.complaint.entity.ComplaintAttachment;
import com.hometalk.onepass.complaint.repository.ComplaintAttachmentRepository;
import com.hometalk.onepass.complaint.repository.ComplaintRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final FileProperties fileProperties;

    /*
     * 민원 등록 (중복 방지 포함)
     */
    @Transactional
    public Long saveWithFiles(ComplaintDto dto, List<MultipartFile> files, CustomUserDetails userDetails) throws IOException {
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저 정보가 없습니다."));

        // 1. 중복 제출 방지 (1분 이내 동일 제목/카테고리)
        if (isDuplicate(dto, user.getId())) {
            throw new IllegalStateException("이미 동일한 민원이 접수되었습니다. 잠시 후 다시 시도해주세요.");
        }

        Complaint complaint = dto.toEntity();
        complaint.assignUser(user);
        complaintRepository.save(complaint);

        // 파일 저장 로직 (기존 유지)
        handleFileUpload(files, complaint);

        return complaint.getId();
    }

    /*
     * 전체 목록 (비밀글 로직 복구)
     */
    public Page<ComplaintDto> findAll(Long userId, boolean isAdmin, Pageable pageable) {
        return complaintRepository.findAll(pageable)
                .map(c -> {
                    ComplaintDto dto = ComplaintDto.fromEntity(c);

                    // 2. 권한 판별 로직
                    boolean isOwner = (userId != null && c.getUser() != null && c.getUser().getId().equals(userId));
                    boolean canView = isOwner || isAdmin;

                    dto.setCanView(canView);
                    dto.setCanEdit(canView);
                    dto.setIsAdmin(isAdmin);

                    // 3. [비밀글 로직] 권한이 없는 경우 제목과 내용을 마스킹합니다.
                    if (Boolean.TRUE.equals(dto.getSecret()) && !canView) {
                        dto.setTitle("🔒 비밀글입니다. (작성자와 관리자만 확인 가능)");
                        dto.setContent(null);
                        dto.setUserName("비공개");
                    }
                    return dto;
                });
    }

    /*
     * 상세 조회 (ResponseStatusException 적용)
     */
    public ComplaintDto getComplaintDetail(Long id, CustomUserDetails userDetails) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 민원을 찾을 수 없습니다."));

        boolean isOwner = complaint.getUser() != null && complaint.getUser().getId().equals(userDetails.getUserId());
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 권한 체크
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        ComplaintDto dto = ComplaintDto.fromEntity(complaint);
        dto.setCanView(true);
        dto.setCanEdit(isOwner || isAdmin);
        dto.setIsAdmin(isAdmin);

        return dto;
    }

    /*
     * 삭제
     */
    @Transactional
    public void delete(Long id, CustomUserDetails userDetails) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 민원을 찾을 수 없습니다."));

        boolean isOwner = complaint.getUser() != null && complaint.getUser().getId().equals(userDetails.getUserId());
        boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        complaintRepository.deleteById(id);
    }

    /**
     * 중복 제출 방지 로직 (1분 기준)
     */
    private boolean isDuplicate(ComplaintDto dto, Long userId) {
        return complaintRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(lastComplaint -> {
                    boolean isSameContent = lastComplaint.getCategory().equals(dto.getCategory())
                            && lastComplaint.getTitle().equals(dto.getTitle());

                    long diffInSeconds = java.time.Duration.between(
                            lastComplaint.getCreatedAt(),
                            java.time.LocalDateTime.now()
                    ).getSeconds();

                    return isSameContent && (diffInSeconds < 60);
                }).orElse(false);
    }

    /**
     * 파일 업로드 처리 로직 (분리)
     */
    private void handleFileUpload(List<MultipartFile> files, Complaint complaint) throws IOException {
        String uploadPath = fileProperties.getUploadPath();
        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadPath);
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                String uuid = UUID.randomUUID().toString();
                String savedName = uuid + "_" + file.getOriginalFilename();
                file.transferTo(new File(uploadPath, savedName));

                attachmentRepository.save(ComplaintAttachment.builder()
                        .originFileName(file.getOriginalFilename())
                        .storedFileName(savedName)
                        .filePath(savedName)
                        .complaint(complaint)
                        .build());
            }
        }
    }

    /*
     * 단건 조회 (공통 메서드)
     */
    public Complaint findOne(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 민원을 찾을 수 없습니다."));
    }

    /*
     * 관리자 답변 등록
     */
    @Transactional
    public void respond(Long id, String response, CustomUserDetails userDetails) {
        // 1. 관리자 권한 확인
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "답변 권한이 없습니다. 관리자만 가능합니다.");
        }

        // 2. 답변 내용 유효성 검사
        if (response == null || response.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변 내용을 입력해주세요.");
        }

        // 3. 민원 조회 및 답변 추가
        Complaint complaint = findOne(id);
        complaint.addResponse(response);
    }
}


