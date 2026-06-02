package com.hometalk.onepass.inquiry.dto;

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

    public static ComplaintAttachmentDto fromEntity(com.hometalk.onepass.inquiry.entity.ComplaintAttachment attachment) {
        return ComplaintAttachmentDto.builder()
                .id(attachment.getId())
                .originFileName(attachment.getOriginFileName())
                .storedFileName(attachment.getStoredFileName())
                .build();
    }
}