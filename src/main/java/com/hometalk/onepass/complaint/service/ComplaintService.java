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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
     * 민원 등록 + 파일 업로드 (통합 버전)
     */
    @Transactional
    public Long saveWithFiles(ComplaintDto dto,
                              List<MultipartFile> files,
                              CustomUserDetails userDetails) throws IOException {

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        Complaint complaint = dto.toEntity();
        complaint.assignUser(user);

        complaintRepository.save(complaint);

        String uploadPath = fileProperties.getUploadPath();

        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadPath);
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String uuid = UUID.randomUUID().toString();
                String savedName = uuid + "_" + file.getOriginalFilename();

                file.transferTo(new File(uploadPath, savedName));

                ComplaintAttachment attach = ComplaintAttachment.builder()
                        .originFileName(file.getOriginalFilename())
                        .storedFileName(savedName)
                        .filePath(savedName)
                        .complaint(complaint)
                        .build();

                attachmentRepository.save(attach);
            }
        }

        return complaint.getId();
    }

    /*
     * 내 민원 목록
     */
    public Page<ComplaintDto> findByUserId(Long userId, Pageable pageable) {
        Page<Complaint> complaints = complaintRepository.findByUser_Id(userId, pageable);
        if (complaints == null) {
            return Page.empty(pageable);
        }
        return complaints.map(ComplaintDto::fromEntity);
    }

    /*
     * 전체 목록 (권한 포함 DTO 세팅)
     */
    public Page<ComplaintDto> findAll(Long userId, boolean isAdmin, Pageable pageable) {
        return complaintRepository.findAll(pageable)
                .map(c -> {
                    ComplaintDto dto = ComplaintDto.fromEntity(c);

                    // 2. 권한 판별 로직
                    boolean isOwner = (userId != null && c.getUser() != null
                            && c.getUser().getId().equals(userId));

                    boolean canView = isOwner || isAdmin;

                    dto.setCanView(canView);
                    dto.setCanEdit(isOwner || isAdmin);
                    dto.setIsAdmin(isAdmin);

                    // 비밀글 처리 로직
                    // 비밀글 + (작성자도 아니고 관리자도 아님) -> 제목만 노출, 나머지는 마스킹
                    if (Boolean.TRUE.equals(dto.getSecret()) && !canView) {
                        dto.setTitle("🔒 비밀글입니다. (작성자와 관리자만 확인 가능)");
                        dto.setContent(null);
                        dto.setUserName("비공개");
                    }
                    // 일반글이거나 권한이 있으면 그대로 반환
                    return dto;
                });
    }

    public Page<ComplaintDto> findAllPublic(Long userId, boolean isAdmin, Pageable pageable) {
        return complaintRepository.findAllBySecretFalse(pageable)
                .map(c -> {
                    ComplaintDto dto = ComplaintDto.fromEntity(c);

                    boolean isOwner = c.getUser() != null
                            && c.getUser().getId().equals(userId);

                    boolean canView = isOwner || isAdmin;

                    dto.setCanView(canView);
                    dto.setCanEdit(canView);
                    dto.setIsAdmin(isAdmin);

                    return dto;
                });
    }

    /*
     * 단건 조회
     */
    public Complaint findOne(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 민원을 찾을 수 없습니다."));
    }

    /*
     * 상세 조회 (권한 체크 + DTO 세팅)
     */
    @Transactional(readOnly = true)
    public ComplaintDto getComplaintDetail(Long id, CustomUserDetails userDetails) {

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 민원을 찾을 수 없습니다."));

        boolean isOwner = complaint.getUser() != null
                && complaint.getUser().getId().equals(userDetails.getUserId());

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean canView = isOwner || isAdmin;

        if (!canView) {
            throw new org.springframework.security.access.AccessDeniedException("권한 없음");
        }

        ComplaintDto dto = ComplaintDto.fromEntity(complaint);

        dto.setCanView(true);
        dto.setCanEdit(isOwner || isAdmin);
        dto.setIsAdmin(isAdmin);

        return dto;
    }

    /*
     * 관리자 답변
     */
    @Transactional
    public void respond(Long id, String response) {
        Complaint complaint = findOne(id);
        complaint.addResponse(response);
    }


    /*
     * 삭제
     */
    @Transactional
    public void delete(Long id, CustomUserDetails userDetails) {

        Complaint complaint = findOne(id);

        boolean isOwner = complaint.getUser() != null
                && complaint.getUser().getId().equals(userDetails.getUserId());
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("권한 없음");
        }

        complaintRepository.deleteById(id);
    }
}