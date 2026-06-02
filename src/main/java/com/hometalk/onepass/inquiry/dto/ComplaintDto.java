package com.hometalk.onepass.inquiry.dto;

import com.hometalk.onepass.inquiry.entity.Complaint;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter // 등록 시 userId 등을 매핑하기 위해 추가하는 것이 편합니다.
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintDto {

    private Long id; // 목록이나 상세조회 시 글 번호가 필요하므로 추가
    private Long userId;
    private String title;
    private String category;
    private String content;
    private boolean isSecret;
    private int viewCount;
    private String status;
    private String answer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 상세 조회 시 파일 목록을 화면에 뿌려주기 위한 필드 추가
    private List<ComplaintAttachmentDto> attachments; // 이 부분에서 오타가 없는지 체크!

    public static ComplaintDto fromEntity(Complaint complaint) {
        return ComplaintDto.builder()
                .id(complaint.getId())
                .userId(complaint.getUser() != null ? complaint.getUser().getId() : null)
                .title(complaint.getTitle())
                .category(complaint.getCategory())
                .content(complaint.getContent())
                .isSecret(complaint.isSecret())
                .viewCount(complaint.getViewCount())
                .status(complaint.getStatus())
                .answer(complaint.getAnswer())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                // 엔티티의 파일 리스트를 DTO 리스트로 변환
                .attachments(complaint.getAttachments() != null ?
                        complaint.getAttachments().stream()
                                .map(ComplaintAttachmentDto::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public Complaint toEntity() {
        return Complaint.builder()
                .title(this.title)
                .content(this.content)
                .category(this.category)
                .isSecret(this.isSecret)
                .status(this.status != null ? this.status : "접수완료") // 기본값 세팅
                .build();
    }
}