package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
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

    private List<String> categoryBgColors;
    private List<String> categoryTextColors;

    public Board toEntity() {
        return Board.builder()
                .name(this.boardName)
                .code(this.boardCode)
                .system(false) // 관리자가 직접 '생성'하는 게시판은 무조건 false로 고정
                .build();
    }
}
