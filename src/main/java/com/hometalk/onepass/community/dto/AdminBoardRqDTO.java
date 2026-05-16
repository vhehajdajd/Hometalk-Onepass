package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "게시판 영문 코드는 영어, 숫자, -, _ 만 가능합니다."
    )
    private String boardCode;

    private List<String> categoryNames;
    private List<@Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "카테고리 영문 코드는 영어, 숫자, -, _ 만 가능합니다."
            )  String> categoryCodes;

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
