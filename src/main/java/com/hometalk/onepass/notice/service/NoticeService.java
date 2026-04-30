package com.hometalk.onepass.notice.service;

import com.hometalk.onepass.auth.entity.LocalAccount;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.LocalAccountRepository;
import com.hometalk.onepass.notice.dto.NoticeDetailResponseDto;
import com.hometalk.onepass.notice.dto.NoticeListResponseDto;
import com.hometalk.onepass.notice.dto.NoticeRequestDto;
import com.hometalk.onepass.notice.entity.Attachment;
import com.hometalk.onepass.notice.entity.Notice;
import com.hometalk.onepass.notice.exception.NoticeNotFoundException;
import com.hometalk.onepass.notice.repository.AttachmentRepository;
import com.hometalk.onepass.notice.repository.NoticeRepository;
import com.hometalk.onepass.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AttachmentRepository attachmentRepository;
    private final LocalAccountRepository localAccountRepository;
    private final ScheduleRepository scheduleRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    // ── 수정일시 처리 ──────────────────────────────
    private LocalDateTime resolveUpdatedAt(Notice notice) {
        if (notice.getUpdatedAt() == null) return null;
        long diff = java.time.Duration.between(notice.getCreatedAt(), notice.getUpdatedAt()).toSeconds();
        if (diff < 1) return null;
        return notice.getUpdatedAt();
    }

    // ── 공지 목록 조회 ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<NoticeListResponseDto> getNoticeList(int page) {
        Pageable pageable = PageRequest.of(page, 10,
                Sort.by("isPinned").descending().and(Sort.by("createdAt").descending()));
        Page<Notice> notices = noticeRepository.findAll(pageable);
        return notices.map(notice -> new NoticeListResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getViewCount(),
                notice.getCreatedAt(),
                resolveUpdatedAt(notice)
        ));
    }

    // ── 공지 작성 ─────────────────────────────────────────────────────────────
    public Long createNotice(NoticeRequestDto noticeRequestDto, MultipartFile file) {
        if (noticeRequestDto.getBadge() == null) {
            throw new IllegalArgumentException("분류를 선택해주세요.");
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        LocalAccount account = localAccountRepository.findByLoginId(auth.getName())
                .orElseThrow(() -> new RuntimeException("로그인 정보를 찾을 수 없습니다."));
        User user = account.getUser();

        Notice notice = new Notice();
        notice.create(
                noticeRequestDto.getTitle(),
                noticeRequestDto.getContent(),
                Boolean.TRUE.equals(noticeRequestDto.getIsPinned()),
                noticeRequestDto.getBadge(),
                user
        );
        noticeRepository.save(notice);

        if (file != null && !file.isEmpty()) {
            saveFile(file, notice);
        }
        return notice.getId();
    }

    // ── 파일 저장 ─────────────────────────────────────────────────────────────
    private void saveFile(MultipartFile file, Notice notice) {
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            String original = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + (original != null ? original : "file");
            String filePath = uploadPath + "/" + fileName;

            file.transferTo(new File(filePath).getAbsoluteFile());

            Attachment attachment = new Attachment(notice, original != null ? original : "file", filePath, file.getSize());
            attachmentRepository.save(attachment);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + e.getMessage(), e);
        }
    }

    // ── 공지 수정 ─────────────────────────────────────────────────────────────
    public Long updateNotice(Long id, NoticeRequestDto noticeRequestDto, MultipartFile file) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        if (noticeRequestDto.getBadge() == null) {
            throw new IllegalArgumentException("분류를 선택해주세요.");
        }

        notice.update(
                noticeRequestDto.getTitle(),
                noticeRequestDto.getContent(),
                Boolean.TRUE.equals(noticeRequestDto.getIsPinned()),
                noticeRequestDto.getBadge()
        );

        if (file != null && !file.isEmpty()) {
            List<Attachment> existing = attachmentRepository.findByNotice(notice);
            for (Attachment att : existing) {
                File attFile = new File(att.getFilePath());
                if (attFile.exists()) attFile.delete();
            }
            attachmentRepository.deleteByNotice(notice);
            saveFile(file, notice);
        }
        return notice.getId();
    }

    // ── 공지 삭제 ─────────────────────────────────────────────────────────────
    public void deleteNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        // 연결된 일정 먼저 삭제
        scheduleRepository.findFirstByNotice(notice)
                .ifPresent(scheduleRepository::delete);

        List<Attachment> attachments = attachmentRepository.findByNotice(notice);
        for (Attachment attachment : attachments) {
            File attFile = new File(attachment.getFilePath());
            if (attFile.exists()) attFile.delete();
        }
        attachmentRepository.deleteByNotice(notice);
        noticeRepository.delete(notice);
    }

    // ── 공지 상세 조회 ──────────────────────────────────────────
    public NoticeDetailResponseDto getNoticeDetail(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        notice.increaseViewCount();

        return new NoticeDetailResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getViewCount(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getCreatedAt(),
                resolveUpdatedAt(notice)
        );
    }

    // ── 이전글 / 다음글 ───────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public NoticeListResponseDto getPreNotice(Long id) {
        return noticeRepository.findFirstByIdLessThanOrderByIdDesc(id)
                .map(notice -> new NoticeListResponseDto(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getBadge(),
                        notice.getIsPinned(),
                        notice.getViewCount(),
                        notice.getCreatedAt(),
                        resolveUpdatedAt(notice)
                ))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public NoticeListResponseDto getNextNotice(Long id) {
        return noticeRepository.findFirstByIdGreaterThanOrderByIdAsc(id)
                .map(notice -> new NoticeListResponseDto(
                        notice.getId(),
                        notice.getTitle(),
                        notice.getBadge(),
                        notice.getIsPinned(),
                        notice.getViewCount(),
                        notice.getCreatedAt(),
                        resolveUpdatedAt(notice)
                ))
                .orElse(null);
    }

    // ── 키워드 검색 ───────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<NoticeListResponseDto> searchNotice(String keyword, String searchType, int page) {
        Pageable pageable = PageRequest.of(page, 10,
                Sort.by("isPinned").descending().and(Sort.by("createdAt").descending()));

        Page<Notice> notices;

        if ("title".equals(searchType)) {
            // 제목만 검색
            notices = noticeRepository.findByTitleContaining(keyword, pageable);
        } else {
            // 제목+내용 검색 (기본값)
            notices = noticeRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable);
        }

        return notices.map(notice -> new NoticeListResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getViewCount(),
                notice.getCreatedAt(),
                resolveUpdatedAt(notice)
        ));
    }

    // ── 첨부파일 조회 ─────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<Attachment> getAttachments(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));
        return attachmentRepository.findByNotice(notice);
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다. id: " + attachmentId));
    }

    // ── 수정 페이지용 공지 조회 ────────────────────────────
    @Transactional(readOnly = true)
    public NoticeDetailResponseDto getNoticeForEdit(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));

        return new NoticeDetailResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getViewCount(),
                notice.getBadge(),
                notice.getIsPinned(),
                notice.getCreatedAt(),
                resolveUpdatedAt(notice)
        );
    }

    // ── 일정 연동용 ──────────────────────────────────
    @Transactional(readOnly = true)
    public Notice getNoticeEntity(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
    }
}