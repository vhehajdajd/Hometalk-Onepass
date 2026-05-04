package com.hometalk.onepass.complaint.dto;

import com.hometalk.onepass.complaint.entity.ComplaintAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComplaintAttachmentDto {
    private Long id;
    private String originFileName;
    private String storedFileName;

    public static ComplaintAttachmentDto fromEntity(ComplaintAttachment attachment) {
        return ComplaintAttachmentDto.builder()
                .id(attachment.getId())
                .originFileName(attachment.getOriginFileName())
                .storedFileName(attachment.getStoredFileName())
                .build();
    }
}