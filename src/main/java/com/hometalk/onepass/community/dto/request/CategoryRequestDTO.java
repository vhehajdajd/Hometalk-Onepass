package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDTO {
    private String name;
    private Long boardId;
    @Pattern(
            regexp = "^[a-zA-Z0-9_-]+$",
            message = "카테고리 영문 코드는 영어, 숫자, -, _ 만 가능합니다."
    )
    private String code;

    public Category toEntity(Board board) {
        return Category.builder()
                .name(this.name)
                .board(board)
                .code(this.code)
                .build();
    }
}
