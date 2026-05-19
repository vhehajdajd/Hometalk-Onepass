package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.enums.ReportReason;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReportRequestDTO {
    private Long postId;
    private ReportReason reason;
    @NotBlank
    private String detail;

}
