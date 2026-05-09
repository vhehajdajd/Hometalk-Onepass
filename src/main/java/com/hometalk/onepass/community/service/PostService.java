package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.CommunityPostResponseDTO;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.PostListResponse;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.dto.response.PostUserRsDTO;
import com.hometalk.onepass.community.entity.*;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.InvalidBoardCodeException;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.repository.BoardRepository;
import com.hometalk.onepass.community.repository.CategoryRepository;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.repository.TagRepository;
import com.hometalk.onepass.community.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostActionService postActionService;
    private final PostValidator postValidator;
    private final CategoryRepository categoryRepository;
    private final BoardRepository boardRepository;
    private final TagRepository tagRepository;

    // Create
    @Transactional
    public Long postSave(String boardCode, PostRequestDTO dto, Long userId) {
        Board board = boardRepository.findByCode(boardCode)
                .orElseThrow(() -> new InvalidBoardCodeException(boardCode));
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
        }
        // 작성자 정보 조회
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Post post;
        if (dto.getId() != null) {
            // [CASE: 수정/등록] ID가 있으면 기존 글을 찾아서 업데이트
            post = postRepository.findById(dto.getId())
                    .orElseThrow(() -> new PostNotFoundException(dto.getId(), boardCode));

            // 작성자 본인인지 확인하는 검증 로직 추가 (보안상 중요!)
            postValidator.validateOwner(post, userId);

            // 기존 엔티티의 필드만 변경 (Dirty Checking으로 자동 반영)
            post.update(dto.getTitle(), dto.getContent(), category, dto.getPostStatus());

            // 기존 태그 관계 초기화
            post.getPostTags().clear();
        } else {
            // [CASE: 신규] ID가 없으면 새로 생성
            post = dto.toEntity(category, board, writer);
            post = postRepository.save(post);
        }

        // 태그
        List<String> tags = dto.getTags() != null ? dto.getTags() : List.of();
        for (String tagName : tags) {
            if (tagName == null) continue;
            String cleanTag = tagName.trim();
            if (cleanTag.isEmpty()) continue;
            Tag tag = tagRepository.findByName(cleanTag)
                    .orElseGet(() -> tagRepository.save(
                            Tag.builder()
                                    .name(cleanTag)
                                    .build()
                    ));
            PostTag postTag = PostTag.builder()
                    .post(post)
                    .tag(tag)
                    .build();
            post.addPostTag(postTag);
        }
        return post.getId();
    }

    // Read
    public Page<PostListResponse> searchPosts(Long boardId, Long categoryId, String searchType, String keyword, int page) {
        PostStatus status = PostStatus.ACTIVE;
        Pageable pageable = PageRequest.of(page, 10,
                Sort.by(
                        Sort.Order.desc("pinned"), // 고정글이 1순위
                        Sort.Order.desc("id")      // 그 안에서 최신순이 2순위
                )
        );

        // 1. 보드 엔티티 조회 (검색 메서드 파라미터가 Board 객체이므로 필요)
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시판입니다."));

        // 2. 검색어나 검색 타입이 없으면 일반 목록 조회
        if (keyword == null || keyword.isBlank()) {
            return getNormalList(boardId, categoryId, status, pageable).map(PostListResponse::new);
        }

        Page<Post> posts;
        // 2. 검색어 존재 여부에 따른 분기 처리
        posts = switch (searchType) {
            case "title" -> postRepository.findByTitle(board, keyword, status, pageable);
            case "nickname" -> postRepository.findByNickname(board, keyword, status, pageable);
            case "tc" -> postRepository.findByTitleOrContent(board, keyword, status, pageable);
            case "tag" -> postRepository.findByTagName(board.getId(), keyword, status, pageable);
            default -> getNormalList(boardId, categoryId, status, pageable);
        };
        return posts.map(PostListResponse::new);
    }
    // 중복 코드를 방지하기 위한 내부 헬퍼 메서드
    private Page<Post> getNormalList(Long boardId, Long categoryId, PostStatus status, Pageable pageable) {
        if (categoryId == null) {
            return postRepository.findActivePosts(boardId, PostStatus.ACTIVE, pageable);
        }
        return postRepository.findCategoryPosts(boardId, categoryId, PostStatus.ACTIVE, pageable);
    }

    // Read - 상세 페이지
    @Transactional
    public PostResponseDTO postDetail(Long postId, PostUserRsDTO currentUser, String boardCode, List<Long> viewedPosts) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException(postId, boardCode));
        Long currentUserId = Optional.ofNullable(currentUser)
                .map(PostUserRsDTO::getUserId)
                .orElse(null);
        postActionService.increaseViewCount(postId, currentUserId, viewedPosts);
        PostResponseDTO dto = new PostResponseDTO(post);
        postValidator.setAuthority(dto, post, currentUser);
        return dto;
    }

    // 임시저장글 개수
    @Transactional(readOnly = true)
    public int getTempPostCount(String boardCode) {
        return postRepository.countByBoardCodeAndPostStatus(boardCode, PostStatus.DRAFT);
    }

    // Update
    // 수정 화면에 데이터를 가져오기
    public PostRequestDTO getPostForEdit(Long id, String boardCode) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException(id, boardCode));

        List<String> tagNames = post.getPostTags().stream()
                .map(postTag -> postTag.getTag().getName())
                .collect(Collectors.toList());

        // 엔티티를 바로 RequestDTO로 변환해서 반환
        return PostRequestDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .postStatus(post.getPostStatus())
                .pinned(post.isPinned())
                .tags(tagNames)
                .build();
    }

    // Delete
    @Transactional
    public void deletePost(Long id, Long currentUserId, String boardCode) {   // user merge 후에는 userId도 필요
        // 게시글 조회
        Post post = postRepository.findById(id).orElseThrow(() -> new PostNotFoundException(id, boardCode));
        // 권한 검증
        postValidator.validateOwner(post, currentUserId);
        post.softDelete();
    }

    public List<PostListResponse> getTempPosts(String boardCode, Long userId) {
        PostStatus status = PostStatus.DRAFT;
        List<Post> posts = postRepository.findTempPosts(boardCode, userId, PostStatus.DRAFT);

        // boardCode와 userId가 일치하고 상태가 DRAFT인 글만 최신순으로 조회
        return posts.stream()
                .map(PostListResponse::new)
                .collect(Collectors.toList());
    }



    // 태그
    public List<String> getTagsByBoardId(Long boardId) {
        return tagRepository.findAllTagNamesByBoardId(boardId);
    }

    public List<String> getTagsByPostId(Long postId) {
        return postRepository.findTagsByPostId(postId);
    }


    @Transactional(readOnly = true)
    public List<CommunityPostResponseDTO> getRecentPosts() {
        List<Post> posts = postRepository
                .findTop5ByPostStatusOrderByCreatedAtDesc(PostStatus.ACTIVE);

        return posts.stream()
                .map(post -> new CommunityPostResponseDTO(
                        post.getId(),
                        post.getTitle(),
                        post.getCategory().getName(),
                        post.getCategory().getCode()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResponseDTO> getPopularPosts() {
        List<Post> posts = postRepository
                .findTop5ByPostStatusOrderByViewCountDesc(PostStatus.ACTIVE);
        return posts.stream()
                .map(post -> new CommunityPostResponseDTO(
                        post.getId(),
                        post.getTitle(),
                        post.getCategory().getName(),
                        post.getCategory().getCode()
                ))
                .toList();
    }
}
