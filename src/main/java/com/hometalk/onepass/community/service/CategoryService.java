package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.response.CategoryResponseDTO;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.exception.CategoryNotFoundException;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;

    @Transactional
    public List<CategoryResponseDTO> findAllByBoardId(Long boardId) {
        return categoryRepository.findAllByBoardId(boardId).stream()
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findByCode(String categoryCode) {
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 코드를 가진 카테고리가 없습니다. code=" + categoryCode));
        return CategoryResponseDTO.from(category);
    }

    // 글쓰기 모드용
    @Transactional
    public List<CategoryResponseDTO> findAllByBoardIdForWrite(Long boardId) {
        return categoryRepository.findAllByBoardId(boardId).stream()
                .filter(category -> !category.getName().equals("전체")) // '전체' 제외
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(Long categoryId, String boardCode) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, boardCode));
        return CategoryResponseDTO.from(category);
    }
}
