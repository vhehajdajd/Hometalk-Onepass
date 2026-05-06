package com.hometalk.onepass.complaint.dto;

import com.hometalk.onepass.complaint.entity.Complaint;
import com.hometalk.onepass.complaint.entity.ComplaintStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintDto {

    private Long id; // 목록이나 상세조회 시 글 번호가 필요하므로 추가
    private Long userId;
    private String userName;
    private String title;
    private String category;
    private String content;
    private Boolean secret;       // 비밀글 여부
    private Integer viewCount;
    private ComplaintStatus status;
    private String answer;

    private Boolean canView;   // 작성자 or ADMIN
    private Boolean canEdit;   // 작성자 or ADMIN
    private Boolean isAdmin;   // ADMIN 여부

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 상세 조회 시 파일 목록을 화면에 뿌려주기 위한 필드 추가
    private List<ComplaintAttachmentDto> attachments;

    public static ComplaintDto fromEntity(Complaint complaint) {

        ComplaintDto dto = ComplaintDto.builder()
                .id(complaint.getId())
                .userId(complaint.getUser() != null ? complaint.getUser().getId() : null)
                .userName(complaint.getUser() != null ? complaint.getUser().getName() : "익명")
                .title(complaint.getTitle())
                .category(complaint.getCategory())
                .content(complaint.getContent())
                .secret(Boolean.TRUE.equals(complaint.getSecret()))
                .viewCount(complaint.getViewCount())
                .status(complaint.getStatus())
                .answer(complaint.getAnswer())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .attachments(complaint.getAttachments() != null ?
                        complaint.getAttachments().stream()
                        .map(ComplaintAttachmentDto::from)
                        .collect(Collectors.toList()) : null)
                .build();

        return dto;
    }

    public Complaint toEntity() {
        return Complaint.builder()
                .title(this.title)
                .content(this.content)
                .category(this.category)
                .secret(this.secret != null ? this.secret : false)
                .viewCount(this.viewCount != null ? this.viewCount : 0)
                .status(this.getStatus())
                .build();
    }
}