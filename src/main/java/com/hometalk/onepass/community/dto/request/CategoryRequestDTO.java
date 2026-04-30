package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
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
    private String code;

    public Category toEntity(Board board) {
        return Category.builder()
                .name(this.name)
                .board(board)
                .code(this.code)
                .build();
    }
}
