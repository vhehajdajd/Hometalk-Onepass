package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.community.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {
    private Long id;
    private String name;
    private String code;
    private Long boardId;

    public static CategoryResponseDTO from(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .boardId(category.getBoard().getId())
                .build();
    }
}
