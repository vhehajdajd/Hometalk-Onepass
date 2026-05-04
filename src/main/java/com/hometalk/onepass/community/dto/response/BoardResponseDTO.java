package com.hometalk.onepass.community.dto.response;

import com.hometalk.onepass.community.entity.Board;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardResponseDTO {
    private Long id;
    private String name;
    private String code;

    // 정적 팩토리 메서드
    public static BoardResponseDTO from(Board board) {
        return BoardResponseDTO.builder()
                .id(board.getId())
                .name(board.getName())
                .code(board.getCode())
                .build();
    }
}
