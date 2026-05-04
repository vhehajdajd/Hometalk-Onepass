package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {
    // 이름으로 게시판 Entity 조회
    Optional<Board> findByName(String name);

    // 코드로 게시판 Entity 조회
    Optional<Board> findByCode(String code);

    boolean existsByCode(String code);

}
