package com.hometalk.onepass.community.dto;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.enums.BoardType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AdminBoardRsDTO {
    private Long id;
    private String name;
    private String code;
    private BoardType boardType;

    // 카테고리 리스트 포함
    private List<CategoryDto> categories;

    private Long visiblePostCount;
    private Long hiddenPostCount;

    private boolean system;

    public static AdminBoardRsDTO from(Board board, List<CategoryDto> categories,
                                       Long visiblePostCount, Long hiddenPostCount) {
        return AdminBoardRsDTO.builder()
                .id(board.getId())
                .name(board.getName())
                .code(board.getCode())
                .boardType(board.getBoardType())
                .categories(categories)
                .visiblePostCount(visiblePostCount)
                .hiddenPostCount(hiddenPostCount)
                .system(board.isSystem())
                .build();
    }

    @Getter @Builder
    public static class CategoryDto {
        private Long id;
        private String name;
        private String code;
        private String bgColor;
        private String textColor;
        private long postCount; // 삭제 가능 여부 판단용
        private boolean system;

        public static CategoryDto from(Category category, long postCount) {
            return CategoryDto.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .code(category.getCode())
                    .bgColor(category.getBgColor())
                    .textColor(category.getTextColor())
                    .postCount(postCount)
                    .system(category.isSystem())
                    .build();
        }
    }
}