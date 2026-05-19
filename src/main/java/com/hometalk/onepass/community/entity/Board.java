package com.hometalk.onepass.community.entity;

import com.hometalk.onepass.community.enums.BoardType;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "boards")
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;        // 한글명

    @Column(unique = true, nullable = false, updatable = false)
    private String code;        // URL용

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardType boardType = BoardType.LIST;

    @Builder.Default
    @Column(name = "is_system", nullable = false)
    private boolean system = false; // 기본값 false, 초기 데이터만 true로 설정

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> categories;

    // 변경 method
    public void changeBoardType(BoardType boardType) {
        if (boardType == null) {
            throw new IllegalArgumentException("게시판 유형은 필수입니다.");
        }
        this.boardType = boardType;
    }

}
