package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);

    // 특정 게시판(boardId)에서 가장 많이 사용된 상위 10개 태그 이름 조회
    @Query("SELECT t.name FROM PostTag pt " +
            "JOIN pt.tag t " +
            "JOIN pt.post p " +
            "WHERE p.board.id = :boardId AND p.postStatus = 'ACTIVE' " +
            "GROUP BY t.name " +
            "ORDER BY COUNT(pt.id) DESC")
    List<String> findTop10TagNamesByBoardId(@Param("boardId") Long boardId, Pageable pageable);

    // 입력한 키워드로 시작하는 태그 최대 5개 검색
    @Query("SELECT t.name FROM Tag t WHERE t.name LIKE :keyword% ORDER BY t.name ASC")
    List<String> findTop5ByNameStartingWith(@Param("keyword") String keyword, Pageable pageable);
}
