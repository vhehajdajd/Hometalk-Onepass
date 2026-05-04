package com.hometalk.onepass.inquiry.dto;

import com.hometalk.onepass.inquiry.entity.InquiryAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryAttachmentDto {
    private Long id;
    private String originFileName;
    private String storedFileName;

    public static InquiryAttachmentDto fromEntity(InquiryAttachment attachment) {
        return InquiryAttachmentDto.builder()
                .id(attachment.getId())
                .originFileName(attachment.getOriginFileName())
                .storedFileName(attachment.getStoredFileName())
                .build();
    }
}