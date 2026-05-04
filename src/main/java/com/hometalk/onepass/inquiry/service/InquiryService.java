package com.hometalk.onepass.inquiry.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import com.hometalk.onepass.inquiry.entity.Inquiry;
import com.hometalk.onepass.inquiry.entity.InquiryAttachment;
import com.hometalk.onepass.inquiry.repository.InquiryAttachmentRepository;
import com.hometalk.onepass.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private final String uploadPath = "C:/onepass/inquiry_uploads/";

    /**
     * 문의 등록 (글 정보 + 파일들 한꺼번에 처리)
     */
    @Transactional
    public Long register(InquiryDto dto, List<MultipartFile> files) throws IOException {
        // 1. 유저 정보 조회
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("작성 유저를 찾을 수 없습니다. ID: " + dto.getUserId()));

        // 2. Inquiry 엔티티 생성 및 저장
        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title(dto.getTitle())
                .category(dto.getCategory())
                .content(dto.getContent())
                .status("미답변") // 초기 상태 세팅
                .build();

        inquiryRepository.save(inquiry);

        // 3. 파일이 있을 경우 파일 저장 및 DB 기록
        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadPath);
            if (!folder.exists()) folder.mkdirs();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String uuid = UUID.randomUUID().toString();
                    String savedName = uuid + "_" + file.getOriginalFilename();

                    // 실제 경로에 파일 저장
                    file.transferTo(new File(uploadPath + savedName));

                    // 첨부파일 엔티티 생성 및 저장
                    InquiryAttachment attach = InquiryAttachment.builder()
                            .originFileName(file.getOriginalFilename())
                            .storedFileName(savedName) // [수정] storedFilePath 대신 storedFileName 사용
                            .filePath(uploadPath + savedName) // [추가] 경로도 따로 저장하도록 필드 맞춰줌
                            .inquiry(inquiry)
                            .build();

                    inquiryAttachmentRepository.save(attach);
                }
            }
        }
        return inquiry.getId();
    }

    // --- 나머지 조회 및 삭제 로직은 동일 ---
    @Transactional(readOnly = true)
    public Page<InquiryDto> findAll(Pageable pageable) {
        return inquiryRepository.findAll(pageable)
                .map(InquiryDto::fromEntity);
    }

    /*
     *  내 민원 리스트 조회 (내 작성글 보기)
     */
    public Page<InquiryDto> findByUserId(Long userId, Pageable pageable) {
        return inquiryRepository.findByUserId(userId, pageable)
                .map(InquiryDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public InquiryDto getInquiryDetail(Long id) {
        return inquiryRepository.findByIdWithAttachments(id)
                .map(InquiryDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("문의글 없음"));
    }

    @Transactional
    public void deleteInquiry(Long id) {
        inquiryRepository.deleteById(id);
    }

    @Transactional
    public void answer(Long id, String answer) {
        Inquiry inquiry = inquiryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 문의글을 찾을 수 없습니다. ID: " + id));
        inquiry.updateAnswer(answer);
    }
}
