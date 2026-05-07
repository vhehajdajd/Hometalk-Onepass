package com.hometalk.onepass.complaint.dto;

import com.hometalk.onepass.complaint.entity.ComplaintAnswer;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintAnswerDto {

    private Long id;
    private String content;
    private String writerName;
    private LocalDateTime createdAt;

    public static ComplaintAnswerDto from(ComplaintAnswer answer) {
        return ComplaintAnswerDto.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .writerName(answer.getWriterName())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}