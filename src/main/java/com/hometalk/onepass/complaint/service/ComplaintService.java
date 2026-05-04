package com.hometalk.onepass.complaint.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.entity.Complaint;
import com.hometalk.onepass.complaint.entity.ComplaintAttachment;
import com.hometalk.onepass.complaint.repository.ComplaintAttachmentRepository;
import com.hometalk.onepass.complaint.repository.ComplaintRepository;

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
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final ComplaintAttachmentRepository attachmentRepository;

    // 파일 저장 경로 (경로 끝에 / 확인!)
    private final String uploadPath = "C:/onepass/complaint_uploads/";

    /*
     * 민원 등록 + 파일 업로드 (통합 버전)
     */
    @Transactional
    public Long saveWithFiles(ComplaintDto dto, List<MultipartFile> files) throws IOException {
        // 1. 유저 정보 조회 (글과 유저 연결)
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("작성 유저를 찾을 수 없습니다. ID: " + dto.getUserId()));

        // 2. DTO -> Entity 변환 및 유저 설정
        Complaint complaint = dto.toEntity();
        complaint.assignUser(user);

        // 3. 민원글 먼저 저장
        complaintRepository.save(complaint);

        // 4. 파일 처리
        if (files != null && !files.isEmpty()) {
            File folder = new File(uploadPath);
            if (!folder.exists()) folder.mkdirs(); // 폴더가 없으면 생성

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String uuid = UUID.randomUUID().toString();
                    String savedName = uuid + "_" + file.getOriginalFilename();

                    // 실제 폴더에 파일 저장
                    file.transferTo(new File(uploadPath + savedName));

                    // 5. DB에 파일 정보 기록
                    ComplaintAttachment attach = ComplaintAttachment.builder()
                            .originFileName(file.getOriginalFilename())
                            .storedFileName(savedName)
                            .filePath(uploadPath + savedName)
                            .complaint(complaint) // 저장한 글과 연결
                            .build();

                    attachmentRepository.save(attach);
                }
            }
        }
        return complaint.getId(); // 저장된 글 번호 반환
    }

    /*
     *  내 민원 리스트 조회 (내 작성글 보기)
     */
    public Page<ComplaintDto> findByUserId(Long userId, Pageable pageable) {
        return complaintRepository.findByUserId(userId, pageable)
                .map(ComplaintDto::fromEntity);
    }

    // --- 기존 조회 및 삭제 로직 ---
    public Page<ComplaintDto> findAll(Pageable pageable) {
        return complaintRepository.findAll(pageable)
                .map(ComplaintDto::fromEntity);
    }

    public Complaint findOne(Long id) {
        return complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 민원을 찾을 수 없습니다."));
    }

    /*
     * 민원 상세 조회 (조회수 증가 포함)
     */
    @Transactional
    public ComplaintDto getComplaintDetail(Long id) {
        // 1. 엔티티 조회
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 민원을 찾을 수 없습니다. ID: " + id));

        // 2. 조회수 증가
        complaint.incrementViewCount();

        // 3. DTO로 변환하여 반환
        return ComplaintDto.fromEntity(complaint);
    }

    @Transactional
    public void respond(Long id, String response) {
        Complaint complaint = findOne(id);
        complaint.addResponse(response);
    }

    @Transactional
    public void delete(Long id) {
        complaintRepository.deleteById(id);
    }
}