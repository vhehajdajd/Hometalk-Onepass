package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.AdminBoardRqDTO;
import com.hometalk.onepass.community.dto.AdminBoardRsDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.exception.CategoryNotFoundException;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityAdminService {
    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    // 게시판&카테고리 생성
    @Transactional
    public void createBoardWithCategories(AdminBoardRqDTO adminBoardRqDTO) {
        // 카테고리 리스트 체크
        if (adminBoardRqDTO.getCategoryNames() == null || adminBoardRqDTO.getCategoryNames().isEmpty()) {
            throw new IllegalArgumentException("1개 이상의 카테고리가 필요합니다.");
        }

        // 게시판 생성
        Board board = Board.builder()
                .name(adminBoardRqDTO.getBoardName())
                .code(adminBoardRqDTO.getBoardCode())
                .build();
        // 게시판 저장
        boardRepository.save(board);

        // 전체(all) 카테고리 자동 생성 (기본값)
        Category allCategory = Category.builder()
                .name("전체")
                .code("all")
                .color("#F5F5F5") // 기본 네이비 색상 등으로 지정
                .board(board)
                .build();
        categoryRepository.save(allCategory);

        // 카테고리 저장
        for (int i = 0; i < adminBoardRqDTO.getCategoryNames().size(); i++) {
            // "all" 중복 생성 방지
            if ("all".equalsIgnoreCase(adminBoardRqDTO.getCategoryCodes().get(i))) continue;
            Category category = Category.builder()
                    .name(adminBoardRqDTO.getCategoryNames().get(i))
                    .code(adminBoardRqDTO.getCategoryCodes().get(i))
                    .color(adminBoardRqDTO.getCategoryColors().get(i))
                    .board(board)
                    .build();
            categoryRepository.save(category);
        }
    }

    // 조회
    @Transactional(readOnly = true)
    public List<AdminBoardRsDTO> getAdminBoardList() {
        return boardRepository.findAll().stream()
                .map(board -> {
                    List<AdminBoardRsDTO.CategoryDto> categories = board.getCategories().stream()
                            .map(cat -> AdminBoardRsDTO.CategoryDto.from(cat, postRepository.countByCategoryId(cat.getId())))
                            .collect(Collectors.toList());
                    return AdminBoardRsDTO.from(board, categories);
                })
                .collect(Collectors.toList());
    }

    // 카테고리 수정
    @Transactional
    public void updateCategoryName(Long categoryId, String newName) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, "ADMIN"));

        // 게시글이 하나라도 있으면 이름 수정도 막고 싶을 때
        if (postRepository.countByCategoryId(categoryId) > 0) {
            throw new IllegalStateException("활동 중인 카테고리는 수정할 수 없습니다.");
        }
        category.rename(newName); // 게시글이 0개일 때만 수정 허용
    }

    // 카테고리 삭제
    @Transactional
    public void deleteCategory(Long categoryId) {
        // 1. 해당 카테고리에 게시글이 존재하는지 확인
        long postCount = postRepository.countByCategoryId(categoryId);
        if (postCount > 0) {
            // 게시글이 있으면 예외 발생
            throw new IllegalStateException("해당 카테고리에 게시글이 존재하여 삭제할 수 없습니다. (현재 " + postCount + "개)");
        }
        // 2. 게시글이 없을 때만 삭제 진행
        categoryRepository.deleteById(categoryId);
    }
}
