package com.hometalk.onepass.community.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminBoardRqDTO {
    private String boardName;
    private String boardCode;

    private List<String> categoryNames;
    private List<String> categoryCodes;
    private List<String> categoryColors;
}
