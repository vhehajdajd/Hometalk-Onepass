package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.AdminBoardRqDTO;
import com.hometalk.onepass.community.dto.AdminBoardRsDTO;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.CategoryNotFoundException;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityAdminService {
    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;

    // 게시판 & 카테고리 전체 목록 조회 (관리자 메인용)
    @Transactional(readOnly = true)
    public List<AdminBoardRsDTO> getAdminBoardList() {
        return boardRepository.findAll().stream()
                .map(board -> {
                    // 각 게시판에 속한 카테고리들을 DTO로 변환
                    List<AdminBoardRsDTO.CategoryDto> categories = board.getCategories().stream()
                            .map(cat -> {
                                // 각 카테고리별 게시글 개수 카운트
                                long postCount = postRepository.countByCategoryId(cat.getId());
                                return AdminBoardRsDTO.CategoryDto.from(cat, postCount);
                            })
                            .collect(Collectors.toList());

                    // 게시판 정보와 카테고리 리스트를 합쳐서 반환
                    return AdminBoardRsDTO.from(board, categories);
                })
                .collect(Collectors.toList());
    }

    // --- [1. 게시판 관리] ---
    @Transactional
    public void createBoard(AdminBoardRqDTO dto) {
        if (boardRepository.count() >= 5) {
            throw new IllegalStateException("게시판은 최대 5개까지만 생성 가능합니다.");
        }
        String code = generateBoardCode(dto.getBoardName());

        if (boardRepository.existsByCode(code)) {
            throw new IllegalStateException("이미 존재하는 게시판 코드입니다: " + code);
        }
        Board board = Board.builder()
                .name(dto.getBoardName())
                .code(code)
                .system(false)
                .build();
        boardRepository.save(board);

        // 기본 '전체' 카테고리 자동 생성
        createDefaultCategory(board);

        // 추가 카테고리들이 있다면 생성
        if (dto.getCategoryNames() != null) {
            for (int i = 0; i < dto.getCategoryNames().size(); i++) {
                createCustomCategory(board, dto.getCategoryNames().get(i));
            }
        }
    }

    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));

        if (board.isSystem()) {
            throw new IllegalStateException("광장, 마켓, 소통 등 시스템 게시판은 삭제할 수 없습니다.");
        }

        // 게시판 삭제 시 하위 카테고리에 글이 있으면 삭제 불가 (제약 조건 활용 혹은 직접 체크)
        boardRepository.delete(board);
    }

    // --- [2. 카테고리 관리] ---

    @Transactional
    public void updateCategory(Long categoryId, String newName) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, "ADMIN"));

        if (category.isSystem()) {
            throw new IllegalStateException("시스템 기본 카테고리는 이름을 수정할 수 없습니다.");
        }

        category.rename(newName);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, "ADMIN"));

        if (category.isSystem()) {
            throw new IllegalStateException("시스템 기본 카테고리는 삭제할 수 없습니다.");
        }

        long postCount = postRepository.countByCategoryId(categoryId);
        if (postCount > 0) {
            throw new IllegalStateException("게시글이 존재하는 카테고리는 삭제할 수 없습니다. (현재 " + postCount + "개)");
        }

        categoryRepository.delete(category);
    }

    // --- [3. 게시글 및 알림 관리] ---

    @Transactional(readOnly = true)
    public List<PostResponseDTO> getAdminManagedPosts() {
        // 숨김(HIDDEN) 또는 삭제(DELETED) 상태의 글만 조회
        List<PostStatus> targets = List.of(PostStatus.HIDDEN, PostStatus.DELETED);
        return postRepository.findAllByPostStatusInOrderByCreatedAtDesc(targets)
                .stream()
                .map(PostResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 영구 삭제
    public void deleteExpiredPosts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<Post> expiredPosts = postRepository.findAllByPostStatusAndUpdatedAtBefore(PostStatus.DELETED, threshold);

        if (!expiredPosts.isEmpty()) {
            postRepository.deleteAll(expiredPosts);
            log.info("관리자 정책: 30일 경과된 삭제글 {}건 영구 삭제 완료", expiredPosts.size());
        }
    }

    // --- [내부 헬퍼 메서드] ---
    private void createDefaultCategory(Board board) {
        categoryRepository.save(Category.builder()
                .name("전체").code("all").system(true).board(board).build());
    }

    private void createCustomCategory(Board board, String name) {
        String code = generateCategoryCode(name);
        if (categoryRepository.existsByCodeAndBoardId(code, board.getId())) {
            throw new IllegalStateException("이미 존재하는 카테고리 코드입니다: " + code);
        }
        categoryRepository.save(Category.builder()
                .name(name).code(code).system(false).board(board).build());
    }

    private String generateBoardCode(String name) {
        return name
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }

    private String generateCategoryCode(String name) {
        return name
                .toLowerCase()
                .trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
    }
}
