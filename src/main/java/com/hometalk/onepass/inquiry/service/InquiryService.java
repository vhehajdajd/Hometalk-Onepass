package com.hometalk.onepass.inquiry.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.complaint.config.FileProperties;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.entity.InquiryAttachment;
import com.hometalk.onepass.inquiry.repository.InquiryAttachmentRepository;
import com.hometalk.onepass.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final InquiryAttachmentRepository inquiryAttachmentRepository;
    private final FileProperties fileProperties;

    /*
     * 문의 등록 (사용자 연동)
     */
    @Transactional
    public Long register(InquiryDto dto,
                         List<MultipartFile> files,
                         CustomUserDetails userDetails) throws IOException {

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("유저 없음"));

        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(dto.getTitle())
                .category(dto.getCategory())
                .content(dto.getContent())
                .status("미답변")
                .build();

        inquiryRepository.save(inquiry);

        String uploadPath = fileProperties.getUploadPath();

        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadPath);
            if (!folder.exists()) folder.mkdirs();

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

        return inquiry.getId();
    }

    /*
     * 전체 목록 (권한 포함)
     */
    public Page<InquiryDto> findAll(Long userId, boolean isAdmin, Pageable pageable) {

        return inquiryRepository.findAll(pageable)
                .map(i -> {

                    InquiryDto dto = InquiryDto.fromEntity(i);

                    boolean isOwner = i.getUser() != null
                            && i.getUser().getId().equals(userId);

                    boolean canView = isOwner || isAdmin;

                    dto.setCanView(canView);
                    dto.setCanEdit(canView);
                    dto.setIsAdmin(isAdmin);

                    if (!canView) {
                        dto.setContent("🔒 비밀글입니다.");
                        dto.setUserName("비공개");
                    }

                    return dto;
                });
    }

    /*
     * 내 글
     */
    public Page<InquiryDto> findByUserId(Long userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable)
                .map(InquiryDto::fromEntity);
    }

    /*
     * 상세 (attachments 안전 로딩)
     */
    public InquiryDto getInquiryDetail(Long id,
                                       CustomUserDetails userDetails) {

        Inquiry inquiry = inquiryRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new RuntimeException("문의 없음"));

        boolean isOwner = inquiry.getUser() != null
                && inquiry.getUser().getId().equals(userDetails.getUserId());

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("권한 없음");
        }

        InquiryDto dto = InquiryDto.fromEntity(inquiry);

        dto.setCanView(true);
        dto.setCanEdit(isOwner || isAdmin);
        dto.setIsAdmin(isAdmin);

        return dto;
    }

    /*
     * 삭제 (권한 체크 추가)
     */
    @Transactional
    public void deleteInquiry(Long id, CustomUserDetails userDetails) {

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 없음"));

        boolean isOwner = inquiry.getUser() != null
                && inquiry.getUser().getId().equals(userDetails.getUserId());

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("권한 없음");
        }

        inquiryRepository.delete(inquiry);
    }

    /*
     * 답변
     */
    @Transactional
    public void answer(Long id, String answer, CustomUserDetails userDetails) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new AccessDeniedException("관리자만 답변 가능");
        }

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("문의 없음"));

        inquiry.updateAnswer(answer);
    }
}
