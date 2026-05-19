package com.hometalk.onepass.community.service;

import com.hometalk.onepass.community.dto.AdminBoardRqDTO;
import com.hometalk.onepass.community.dto.AdminBoardRsDTO;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Category;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.BoardType;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.CategoryNotFoundException;
import com.hometalk.onepass.community.exception.InvalidBoardCodeException;
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
        return boardRepository.findAllWithCategories().stream()
                .map(board -> {
                    // 각 게시판 카테고리 목록
                    List<AdminBoardRsDTO.CategoryDto> categories = board.getCategories().stream()
                            .map(cat -> {
                                // 각 카테고리별 게시글 개수 카운트
                                long postCount = postRepository.countByCategoryId(cat.getId());
                                return AdminBoardRsDTO.CategoryDto.from(cat, postCount);
                            })
                            .collect(Collectors.toList());

                    // 게시판별 노출 게시글 수
                    long visibleCount = postRepository.countByBoardAndPostStatus(board, PostStatus.ACTIVE);
                    // 게시판별 숨김/삭제 게시글 수
                    long hiddenCount = postRepository.countByBoardAndPostStatusIn(board, List.of(PostStatus.HIDDEN, PostStatus.DELETED));

                    // 게시판 정보와 카테고리 리스트를 합쳐서 반환
                    return AdminBoardRsDTO.from(board, categories, visibleCount, hiddenCount);
                })
                .collect(Collectors.toList());
    }

    // --- [1. 게시판 관리] ---
    @Transactional
    public void createBoard(AdminBoardRqDTO dto) {
        if (boardRepository.count() >= 5) {
            throw new IllegalStateException("게시판은 최대 5개까지만 생성 가능합니다.");
        }
        String code = dto.getBoardCode();

        if (boardRepository.existsByCode(code)) {
            throw new IllegalStateException("이미 존재하는 게시판 코드입니다: " + code);
        }
        Board board = Board.builder()
                .name(dto.getBoardName())
                .code(code)
                .boardType(dto.getBoardType() != null ? dto.getBoardType() : BoardType.LIST)
                .system(false)
                .build();
        boardRepository.save(board);

        // 추가 카테고리들이 있다면 생성
        if (dto.getCategoryNames() != null && dto.getCategoryCodes() != null) {
            for (int i = 0; i < dto.getCategoryNames().size(); i++) {
                String catName = dto.getCategoryNames().get(i);
                String catCode = dto.getCategoryCodes().get(i);
                String bgColor = (dto.getCategoryBgColors() != null && dto.getCategoryBgColors().size() > i)
                        ? dto.getCategoryBgColors().get(i) : "#003366"; // 기본값 딥블루

                String textColor = (dto.getCategoryTextColors() != null && dto.getCategoryTextColors().size() > i)
                        ? dto.getCategoryTextColors().get(i) : "#FFFFFF"; // 기본값 화이트
                createCustomCategory(board, catName, catCode, bgColor, textColor);
            }
        }
    }

    @Transactional
    public void deleteBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new InvalidBoardCodeException(String.valueOf(boardId)));
        long totalPostCount = postRepository.countAllByBoardIdNative(boardId);
        if (totalPostCount > 0) {
            throw new IllegalStateException("이 게시판에 아직 삭제되지 않은 데이터(유령 게시글 등)가 "
                    + totalPostCount + "개 남아있습니다. 게시글 관리 탭에서 모두 영구 삭제해주세요.");
        }
        // 게시판 삭제 시 하위 카테고리에 글이 있으면 삭제 불가 (제약 조건 활용 혹은 직접 체크)
        boardRepository.delete(board);
    }

    // --- [2. 카테고리 관리] ---
    @Transactional
    public void updateCategory(Long categoryId, String newName,
                               String bgColor, String textColor) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, "ADMIN"));
        if (category.isSystem()) {
            throw new IllegalStateException("시스템 기본 카테고리는 이름을 수정할 수 없습니다.");
        }

        String finalBgColor = (bgColor != null && !bgColor.isBlank())
                ? bgColor
                : category.getBgColor();

        String finalTextColor = (textColor != null && !textColor.isBlank())
                ? textColor
                : category.getTextColor();

        category.rename(newName, finalBgColor, finalTextColor);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId, "ADMIN"));
        if (category.isSystem()) {
            throw new IllegalStateException("시스템 기본 카테고리는 삭제할 수 없습니다.");
        }

        long totalPostCount = postRepository.countAllByCategoryId(categoryId);
        if (totalPostCount > 0) {
            throw new IllegalStateException("해당 카테고리에 게시글(숨김/삭제 포함)이 " + totalPostCount + "개 존재합니다. " +
                    "관리자 페이지의 '숨김/삭제 게시글 관리' 메뉴에서 해당 글들을 먼저 영구 삭제해주세요.");
        }
        categoryRepository.delete(category);
    }

    // --- [3. 게시글 및 알림 관리] ---
    @Transactional(readOnly = true)
    public List<PostResponseDTO> getAdminManagedPosts() {
        // 숨김(HIDDEN) 또는 삭제(DELETED) 상태의 글만 조회
        List<String> targets = List.of(PostStatus.HIDDEN.name(), PostStatus.DELETED.name());
        return postRepository.findAllManagedPostsNative(targets)
                .stream()
                .map(PostResponseDTO::from)
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void hardDeleteOldPosts() {
        // 현재 시간으로부터 30일 전 시점 계산
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        // 1. 30일이 지난 DELETED 상태의 게시글 조회
        List<Post> targets = postRepository.findOldDeletedPosts(PostStatus.DELETED, thirtyDaysAgo);

        if (!targets.isEmpty()) {
            for (Post post : targets) {
                // 자식 댓글부터 영구 삭제 후 게시글 삭제
                postRepository.hardDeleteCommentsByPostId(post.getId());
                postRepository.hardDeletePostById(post.getId());
            }
            log.info("관리자 정책: 30일 경과된 게시글 {}건 및 관련 댓글 영구 삭제 완료", targets.size());
        }
    }

    /*
       관리자 권한: 게시글 DB 영구 삭제 (Hard Delete)
       카테고리 삭제 전, 해당 카테고리의 글들을 완전히 지울 때 사용
     */
    @Transactional
    public void hardDeletePost(Long postId) {
        // DB에서 실제 레코드를 삭제 (FK 제약 조건을 풀기 위한 작업)
        postRepository.hardDeletePostTagsByPostId(postId);  // 태그 관계 삭제
        postRepository.hardDeleteCommentsByPostId(postId);  // 댓글 삭제
        postRepository.hardDeletePostById(postId);          // 게시글 삭제
        log.info("관리자 권한으로 게시글 영구 삭제 완료: ID {}", postId);
    }
    // 일괄 처리
    @Transactional
    public void hardDeletePosts(List<Long> postIds) {
        if (postIds != null && !postIds.isEmpty()) {
            postIds.forEach(this::hardDeletePost);
            log.info("관리자 권한으로 게시글 {}건 일괄 영구 삭제 완료", postIds.size());
        }
    }

    private void createCustomCategory(Board board, String name, String code,
                                      String bgColor, String textColor) {
        if ("all".equalsIgnoreCase(code)) {
            throw new IllegalStateException("'all'은 전체 목록 URL 예약어라 카테고리 코드로 사용할 수 없습니다.");
        }
        if (categoryRepository.existsByCodeAndBoardId(code, board.getId())) {
            throw new IllegalStateException("해당 게시판 내에 중복된 카테고리 코드가 있습니다: " + code);
        }
        categoryRepository.save(Category.builder()
                .name(name)
                .code(code)
                .bgColor(bgColor)
                .textColor(textColor)
                .system(false)
                .board(board)
                .build());
    }

    @Transactional(readOnly = true)
    public AdminBoardRsDTO getAdminBoardDetail(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new InvalidBoardCodeException(String.valueOf(id)));

        List<AdminBoardRsDTO.CategoryDto> categories = board.getCategories().stream()
                .map(cat -> {
                    long postCount = postRepository.countByCategoryId(cat.getId());
                    return AdminBoardRsDTO.CategoryDto.from(cat, postCount);
                })
                .collect(Collectors.toList());
        // 노출 게시글 수
        long visibleCount = postRepository.countByBoardAndPostStatus(board, PostStatus.ACTIVE);
        // 숨김/삭제 게시글 수
        long hiddenCount = postRepository.countByBoardAndPostStatusIn(board, List.of(PostStatus.HIDDEN, PostStatus.DELETED));

        return AdminBoardRsDTO.from(board, categories, visibleCount, hiddenCount);
    }

    @Transactional
    public void addCategory(Long boardId, String name, String code,
                            String bgColor, String textColor) {
        // 1. 게시판 존재 여부 확인
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new InvalidBoardCodeException(String.valueOf(boardId)));
        long customCategoryCount = board.getCategories().stream().count();
        if (customCategoryCount >= 5) {
            throw new IllegalStateException("추가 카테고리는 게시판당 최대 5개까지만 생성 가능합니다.");
        }
        // 2. 카테고리 이름 중복 체크
        boolean isDuplicate = board.getCategories().stream()
                .anyMatch(c -> c.getName().equals(name));
        if (isDuplicate) {
            throw new IllegalStateException("해당 게시판에 이미 동일한 이름의 카테고리가 존재합니다.");
        }

        // 3. 카테고리 엔티티 생성 및 연관관계 설정
        Category category = Category.builder()
                .name(name).code(code)
                .bgColor(bgColor).textColor(textColor)
                .board(board)  // 부모 게시판 설정
                .system(false) // 사용자가 추가하는 건 시스템 카테고리가 아님
                .build();

        // 4. 저장
        categoryRepository.save(category);
    }

    @Transactional
    public void updateBoardType(Long boardId, BoardType boardType) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new InvalidBoardCodeException(String.valueOf(boardId)));

        board.changeBoardType(boardType);
    }
}
