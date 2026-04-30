package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findAllByBoardId(Long boardId);

    // 코드로 카테고리 Entity 조회
    Optional<Category> findByCode(String code);

    boolean existsByCodeAndBoardId(String code, Long boardId);
}
