package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.enums.BoardType;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BoardRequestDTO {
    private Long id;        // 수정 목적
    private String name;
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "게시판 영문 코드는 영어, 숫자, -, _ 만 가능합니다."
    )
    private String code;
    private BoardType boardType;

    public Board toEntity() {
        return Board.builder()
                .name(this.name).code(this.code)
                .boardType(this.boardType != null ? this.boardType : BoardType.LIST)
                .build();
    }
}
