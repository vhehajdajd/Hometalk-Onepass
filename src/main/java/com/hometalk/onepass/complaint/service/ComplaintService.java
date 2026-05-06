package com.hometalk.onepass.complaint.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
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
import org.springframework.security.core.Authentication;
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
    private final LocalAccountRepository localAccountRepository;
    private final ComplaintAttachmentRepository attachmentRepository;
    private final FileProperties fileProperties;

    @Transactional
    public Long saveWithFiles(ComplaintDto dto,
                              List<MultipartFile> files,
                              Authentication authentication) throws IOException {

        Long userId = getLoginUserId(authentication);

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "유저 정보가 없습니다."));

        if (isDuplicate(dto, user.getId())) {
            throw new IllegalStateException("이미 동일한 민원이 접수되었습니다. 잠시 후 다시 시도해주세요.");
        }

        Complaint complaint = dto.toEntity();
        complaint.assignUser(user);

        complaintRepository.save(complaint);

        handleFileUpload(files, complaint);

        return complaint.getId();
    }

    public Page<ComplaintDto> findAll(Long userId,
                                      boolean isAdmin,
                                      Pageable pageable) {

        return complaintRepository.findAll(pageable)
                .map(c -> {

                    ComplaintDto dto = ComplaintDto.fromEntity(c);

                    boolean isOwner =
                            userId != null
                                    && c.getUser() != null
                                    && c.getUser().getId().equals(userId);

                    boolean canView = isOwner || isAdmin;

                    dto.setCanView(canView);
                    dto.setCanEdit(canView);
                    dto.setIsAdmin(isAdmin);

                    if (Boolean.TRUE.equals(dto.getSecret()) && !canView) {
                        dto.setTitle("🔒 비밀글입니다. (작성자와 관리자만 확인 가능)");
                        dto.setContent(null);
                        dto.setUserName("비공개");
                    }

                    return dto;
                });
    }

    /*
     * 상세 조회
     */
    public ComplaintDto getComplaintDetail(Long id,
                                           Authentication authentication) {

        Long userId = getLoginUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "해당 민원을 찾을 수 없습니다."));

        boolean isOwner =
                complaint.getUser() != null
                        && complaint.getUser().getId().equals(userId);

        if (Boolean.TRUE.equals(complaint.getSecret())
                && !isOwner
                && !isAdmin) {

            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "비밀글 접근 권한이 없습니다.");
        }

        ComplaintDto dto = ComplaintDto.fromEntity(complaint);

        dto.setCanView(true);
        dto.setCanEdit(isOwner || isAdmin);
        dto.setIsAdmin(isAdmin);

        return dto;
    }

    /*
     * 내 민원 목록
     */
    public Page<ComplaintDto> findByUserId(Long userId,
                                           Pageable pageable) {

        return complaintRepository.findByUserId(userId, pageable)
                .map(c -> {

                    ComplaintDto dto = ComplaintDto.fromEntity(c);

                    dto.setCanView(true);
                    dto.setCanEdit(true);
                    dto.setIsAdmin(false);

                    return dto;
                });
    }

    @Transactional
    public void delete(Long id,
                       Authentication authentication) {

        Long userId = getLoginUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "삭제할 민원을 찾을 수 없습니다."));

        boolean isOwner =
                complaint.getUser() != null
                        && complaint.getUser().getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "삭제 권한이 없습니다.");
        }

        complaintRepository.deleteById(id);
    }

    public Complaint findOne(Long id) {

        return complaintRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "해당 민원을 찾을 수 없습니다."));
    }

    @Transactional
    public void respond(Long id,
                        String response,
                        Authentication authentication) {

        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "답변 권한이 없습니다. 관리자만 가능합니다.");
        }

        if (response == null || response.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "답변 내용을 입력해주세요.");
        }

        Complaint complaint = findOne(id);

        complaint.addResponse(response);
    }

    public Long getLoginUserId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {

            if (customUserDetails.getUserId() != null) {
                return customUserDetails.getUserId();
            }
        }

        String loginId = authentication.getName();

        LocalAccount account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "사용자 정보를 찾을 수 없습니다."));

        return account.getUser().getId();
    }

    public boolean isAdmin(Authentication authentication) {

        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(a ->
                        a.getAuthority().equals("ROLE_ADMIN")
                                || a.getAuthority().equals("ADMIN")
                );
    }

    private boolean isDuplicate(ComplaintDto dto,
                                Long userId) {

        return complaintRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(lastComplaint -> {

                    boolean isSameContent =
                            lastComplaint.getCategory().equals(dto.getCategory())
                                    && lastComplaint.getTitle().equals(dto.getTitle());

                    long diffInSeconds =
                            java.time.Duration.between(
                                    lastComplaint.getCreatedAt(),
                                    java.time.LocalDateTime.now()
                            ).getSeconds();

                    return isSameContent && (diffInSeconds < 60);

                }).orElse(false);
    }

    private void handleFileUpload(List<MultipartFile> files,
                                  Complaint complaint) throws IOException {

        String uploadPath = fileProperties.getPath();

        if (files != null && !files.isEmpty()) {

            File folder = new File(uploadPath);

            if (!folder.exists()) {
                folder.mkdirs();
            }

            for (MultipartFile file : files) {

                if (file.isEmpty()) continue;

                String uuid = UUID.randomUUID().toString();

                String savedName =
                        uuid + "_" + file.getOriginalFilename();

                file.transferTo(new File(uploadPath, savedName));

                attachmentRepository.save(
                        ComplaintAttachment.builder()
                                .originFileName(file.getOriginalFilename())
                                .storedFileName(savedName)
                                .filePath(savedName)
                                .complaint(complaint)
                                .build()
                );
            }
        }
    }
}