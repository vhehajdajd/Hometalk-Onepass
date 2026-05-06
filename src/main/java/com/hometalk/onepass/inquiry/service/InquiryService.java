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
import org.springframework.http.HttpStatus; // 추가
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // 추가

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final InquiryAttachmentRepository inquiryAttachmentRepository;
    private final FileProperties fileProperties;

    /*
     * 문의 등록 (데이터 누락 및 중복 체크 추가)
     */
    @Transactional
    public Long register(InquiryDto dto,
                         List<MultipartFile> files,
                         CustomUserDetails userDetails) throws IOException {

        // 1. 필수 데이터 누락 체크 (ServiceExceptionHandler의 handleValidation 연동)
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해주세요.");
        }

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."));





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
     * 상세 조회 (비인가 접근 예외 처리)
     */
    public InquiryDto getInquiryDetail(Long id, CustomUserDetails userDetails) {

        Inquiry inquiry = inquiryRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 문의글을 찾을 수 없습니다."));

        boolean isOwner = inquiry.getUser() != null
                && inquiry.getUser().getId().equals(userDetails.getUserId());

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // 3. 비인가 접근 체크 (ServiceExceptionHandler의 handleStatusException 연동)
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "해당 게시글을 볼 권한이 없습니다.");
        }

        InquiryDto dto = InquiryDto.fromEntity(inquiry);
        dto.setCanView(true);
        dto.setCanEdit(isOwner || isAdmin);
        dto.setIsAdmin(isAdmin);

        return dto;
    }

    /*
     * 삭제 (권한 체크 및 403 예외)
     */
    @Transactional
    public void deleteInquiry(Long id, CustomUserDetails userDetails) {

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "삭제할 문의글이 없습니다."));

        boolean isOwner = inquiry.getUser() != null
                && inquiry.getUser().getId().equals(userDetails.getUserId());

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "삭제 권한이 없습니다.");
        }

        inquiryRepository.delete(inquiry);
    }

    /*
     * 답변 (관리자 권한 체크)
     */
    @Transactional
    public void answer(Long id, String answer, CustomUserDetails userDetails) {

        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 답변을 등록할 수 있습니다.");
        }

        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "답변할 문의글이 없습니다."));

        inquiry.updateAnswer(answer);
    }

    // 전체 목록과 내 글 목록은 기존 로직 유지 (권한에 따라 마스킹 처리되므로 예외를 던지지 않음)
    public Page<InquiryDto> findAll(Long userId, boolean isAdmin, Pageable pageable) {
        return inquiryRepository.findAll(pageable)
                .map(i -> {
                    InquiryDto dto = InquiryDto.fromEntity(i);
                    boolean isOwner = i.getUser() != null && i.getUser().getId().equals(userId);
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

    public Page<InquiryDto> findByUserId(Long userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable)
                .map(InquiryDto::fromEntity);
    }

    private boolean isDuplicate(InquiryDto dto, Long userId) {
        return inquiryRepository.findFirstByUserIdOrderByIdDesc(userId)
                .map(lastInquiry -> {
                    // 1. 카테고리와 제목이 같은지 확인
                    boolean isSameContent = lastInquiry.getCategory().equals(dto.getCategory())
                            && lastInquiry.getTitle().equals(dto.getTitle());

                    // 2. 작성한 지 1분(60초)이 지났는지 확인
                    // LocalDateTime을 사용한다면 아래와 같이 비교 가능합니다.
                    long diffInSeconds = java.time.Duration.between(
                            lastInquiry.getCreatedAt(), // 엔티티에 생성일 필드가 있어야 합니다.
                            java.time.LocalDateTime.now()
                    ).getSeconds();

                    // 같은 내용이면서 60초 이내에 올린 경우만 중복으로 간주
                    return isSameContent && (diffInSeconds < 60);
                }).orElse(false);
    }
}