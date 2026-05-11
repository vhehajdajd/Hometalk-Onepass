package com.hometalk.onepass.inquiry.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.complaint.config.FileProperties;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.entity.InquiryAttachment;
import com.hometalk.onepass.inquiry.repository.InquiryAttachmentRepository;
import com.hometalk.onepass.inquiry.repository.InquiryRepository;
import com.hometalk.onepass.notification.entity.NotificationTargetRole;
import com.hometalk.onepass.notification.entity.NotificationType;
import com.hometalk.onepass.notification.publisher.NotificationPublisher;
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
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final LocalAccountRepository localAccountRepository;
    private final InquiryAttachmentRepository inquiryAttachmentRepository;
    private final FileProperties fileProperties;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public Long register(InquiryDto dto,
                         List<MultipartFile> files,
                         Authentication authentication) throws IOException {

        Long userId = getLoginUserId(authentication);

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해주세요.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));

        if (isDuplicate(dto, user.getId())) {
            throw new IllegalStateException("이미 동일한 문의가 접수되었습니다. 잠시 후 다시 시도해주세요.");
        }

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(dto.getTitle())
                .category(dto.getCategory())
                .content(dto.getContent())
                .status("미답변")
                .build();

        inquiryRepository.save(inquiry);

        String uploadPath = fileProperties.getPath();

        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String uuid = UUID.randomUUID().toString();
                String savedName = uuid + "_" + file.getOriginalFilename();

                file.transferTo(new File(uploadPath, savedName));

                InquiryAttachment attach = InquiryAttachment.builder()
                        .originFileName(file.getOriginalFilename())
                        .storedFileName(savedName)
                        .filePath(uploadPath + savedName)
                        .inquiry(inquiry)
                        .build();

                inquiryAttachmentRepository.save(attach);
            }
        }

// ✅ 알림 추가 — 전체 관리자에게 문의 접수 알림
        notificationPublisher.publishToAll(
                NotificationTargetRole.ADMIN,
                NotificationType.INQUIRY_RECEIVED,
                "새로운 문의 접수",
                "새로운 문의가 접수되었습니다.",
                "/inquiries/detail/" + inquiry.getId()
        );

        return inquiry.getId();
    }

    public InquiryDto getInquiryDetail(Long id, Authentication authentication) {

        Long userId = getLoginUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        Inquiry inquiry = inquiryRepository.findByIdWithAttachments(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 문의글을 찾을 수 없습니다."));

        boolean isOwner = inquiry.getUser() != null
                && inquiry.getUser().getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
        }

        InquiryDto dto = InquiryDto.fromEntity(inquiry);
        dto.setCanView(true);
        dto.setCanEdit(isOwner || isAdmin);
        dto.setIsAdmin(isAdmin);

        return dto;
    }

    @Transactional
    public void deleteInquiry(Long id, Authentication authentication) {

        Long userId = getLoginUserId(authentication);
        boolean isAdmin = isAdmin(authentication);

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 문의글이 없습니다."));

        boolean isOwner = inquiry.getUser() != null
                && inquiry.getUser().getId().equals(userId);

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        inquiryRepository.delete(inquiry);
    }

    @Transactional
    public void answer(Long id, String answer, Authentication authentication) {

        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 답변을 등록할 수 있습니다.");
        }

        if (answer == null || answer.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "답변 내용을 입력해주세요.");
        }

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "답변할 문의글이 없습니다."));

        inquiry.updateAnswer(answer);

// ✅ 알림 추가 — 문의 작성자에게 답변 알림
        if (inquiry.getUser() != null) {
            notificationPublisher.publish(
                    inquiry.getUser().getId(),
                    NotificationTargetRole.RESIDENT,
                    NotificationType.INQUIRY_STATUS,
                    "문의 답변 등록",
                    "문의하신 글에 답변이 달렸습니다.",
                    "/inquiries/detail/" + inquiry.getId(),
                    inquiry.getId()
            );
        }
    }

    public Page<InquiryDto> findAll(Long userId, boolean isAdmin, Pageable pageable) {

        if (isAdmin) {
            return inquiryRepository.findAll(pageable)
                    .map(i -> {
                        InquiryDto dto = InquiryDto.fromEntity(i);
                        dto.setCanView(true);
                        dto.setCanEdit(true);
                        dto.setIsAdmin(true);
                        return dto;
                    });
        }

        return inquiryRepository.findByUserId(userId, pageable)
                .map(i -> {
                    InquiryDto dto = InquiryDto.fromEntity(i);
                    dto.setCanView(true);
                    dto.setCanEdit(true);
                    dto.setIsAdmin(false);
                    return dto;
                });
    }

    public Page<InquiryDto> findByUserId(Long userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable)
                .map(i -> {
                    InquiryDto dto = InquiryDto.fromEntity(i);
                    dto.setCanView(true);
                    dto.setCanEdit(true);
                    dto.setIsAdmin(false);
                    return dto;
                });
    }

    public Long getLoginUserId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        String loginId = authentication.getName();

        LocalAccount account = localAccountRepository.findByLoginId(loginId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다."));

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

    private boolean isDuplicate(InquiryDto dto, Long userId) {

        return inquiryRepository.findFirstByUser_IdOrderByIdDesc(userId)
                .map(lastInquiry -> {
                    boolean isSameContent =
                            lastInquiry.getCategory().equals(dto.getCategory())
                                    && lastInquiry.getTitle().equals(dto.getTitle());

                    long diffInSeconds = java.time.Duration.between(
                            lastInquiry.getCreatedAt(),
                            java.time.LocalDateTime.now()
                    ).getSeconds();

                    return isSameContent && diffInSeconds < 60;
                }).orElse(false);
    }

    public List<InquiryDto> findMyRecent(Authentication authentication) {

        Long userId = getLoginUserId(authentication);

        return inquiryRepository.findTop5ByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(InquiryDto::fromEntity)
                .toList();
    }

    public List<InquiryDto> findAdminRecent(Authentication authentication) {

        if (!isAdmin(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }

        return inquiryRepository.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(InquiryDto::fromEntity)
                .toList();
    }
}