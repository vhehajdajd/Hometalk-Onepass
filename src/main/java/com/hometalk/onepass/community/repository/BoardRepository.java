package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Board;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    @EntityGraph(attributePaths = {"categories"})
    @Query("SELECT b FROM Board b")
    List<Board> findAllWithCategories();

    // 코드로 게시판 Entity 조회
    Optional<Board> findByCode(String code);

    boolean existsByCode(String code);

}
