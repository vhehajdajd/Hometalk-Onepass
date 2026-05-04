package com.hometalk.onepass.community.dto.request;

import com.hometalk.onepass.community.entity.Board;
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
    private String code;

    public Board toEntity() {
        return Board.builder()
                .name(this.name).code(this.code)
                .build();
    }
}
