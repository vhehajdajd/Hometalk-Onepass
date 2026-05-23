package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    // 특정 게시판(boardId)에서 사용된 모든 태그 이름 조회
    @Query("SELECT DISTINCT t.name FROM PostTag pt " +
            "JOIN pt.tag t " +
            "JOIN pt.post p " +
            "WHERE p.board.id = :boardId AND p.postStatus = 'ACTIVE'")
    List<String> findAllTagNamesByBoardId(@Param("boardId") Long boardId);
}
