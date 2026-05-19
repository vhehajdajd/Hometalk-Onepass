package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.response.CategoryResponseDTO;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.exception.CategoryNotFoundException;
import com.hometalk.onepass.community.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<CategoryResponseDTO> findAllByBoardId(Long boardId) {
        return categoryRepository.findAllByBoardId(boardId).stream()
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());
    }

    public CategoryResponseDTO findByCode(String categoryCode) {
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new CategoryNotFoundException(categoryCode));
        return CategoryResponseDTO.from(category);
    }

    // 글쓰기 모드용
    @Transactional
    public List<CategoryResponseDTO> findAllByBoardIdForWrite(Long boardId) {
        return categoryRepository.findAllByBoardId(boardId).stream()
                .map(CategoryResponseDTO::from)
                .collect(Collectors.toList());
    }

    public CategoryResponseDTO findById(Long categoryId, String boardCode) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, boardCode));
        return CategoryResponseDTO.from(category);
    }

}
