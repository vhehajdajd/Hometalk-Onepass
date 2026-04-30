package com.hometalk.onepass.community.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "categories",
       uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_category_board_id_code",
            columnNames = {"board_id", "code"}      // 특정 게시판 안에서만 코드 unique
        )})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, updatable = false)
    private String code;

    private String color;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", referencedColumnName = "id", nullable = false)
    private Board board;

    // 변경 method
    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다.");
        }
        this.name = newName;
    }
}