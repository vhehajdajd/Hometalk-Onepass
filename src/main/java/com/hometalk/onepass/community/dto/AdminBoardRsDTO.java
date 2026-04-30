package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminBoardRsDTO {
    private Long id;
    private String name;
    private String code;
    // 카테고리 리스트 포함
    private List<CategoryDto> categories;

    public static AdminBoardRsDTO from(Board board, List<CategoryDto> categories) {
        return AdminBoardRsDTO.builder()
                .id(board.getId())
                .name(board.getName())
                .code(board.getCode())
                .categories(categories)
                .build();
    }

    @Getter @Builder
    public static class CategoryDto {
        private Long id;
        private String name;
        private String code;
        private String color;
        private long postCount; // 삭제 가능 여부 판단용

        public static CategoryDto from(Category category, long postCount) {
            return CategoryDto.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .code(category.getCode())
                    .color(category.getColor())
                    .postCount(postCount)
                    .build();
        }
    }
}