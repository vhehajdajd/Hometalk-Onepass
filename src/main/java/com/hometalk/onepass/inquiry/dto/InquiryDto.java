package com.hometalk.onepass.inquiry.dto;

import com.hometalk.onepass.inquiry.entity.Inquiry;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter // 등록 시 데이터 바인딩을 위해 추가
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InquiryDto {
    private Long id; // 상세보기 링크 이동을 위해 필수!
    private Long userId;
    private String title;
    private String category;
    private String content;
    private String answer;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 상세 조회 시 파일 목록을 화면에 전달하기 위한 필드
    private List<InquiryAttachmentDto> attachments;

    public static InquiryDto fromEntity(Inquiry inquiry) {
        return InquiryDto.builder()
                .id(inquiry.getId())
                .userId(inquiry.getUser() != null ? inquiry.getUser().getId() : null)
                .title(inquiry.getTitle())
                .category(inquiry.getCategory())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                // 엔티티의 InquiryAttachment 리스트를 DTO 리스트로 변환
                .attachments(inquiry.getAttachments() != null ?
                        inquiry.getAttachments().stream()
                                .map(InquiryAttachmentDto::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }

    public Inquiry toEntity() {
        return Inquiry.builder()
                .title(this.title)
                .content(this.content)
                .category(this.category)
                .status(this.status != null ? this.status : "미답변")
                .build();
    }
}