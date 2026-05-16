package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.CommunityPostResponseDTO;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.PostListResponse;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.dto.response.PostUserRsDTO;
import com.hometalk.onepass.community.entity.*;
import com.hometalk.onepass.community.enums.MarketStatus;
import com.hometalk.onepass.community.enums.PostFileType;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.enums.TradeStatus;
import com.hometalk.onepass.community.exception.InvalidBoardCodeException;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.repository.*;
import com.hometalk.onepass.community.validator.PostValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostFileRepository postFileRepository;
    private final FileService fileService;
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
            // 작성자 본인인지 확인
            postValidator.validateOwner(post, userId);
            // 기존 엔티티 필드
            post.update(dto.getTitle(), dto.getContent(), category, dto.getPostStatus());

            // 기존 태그 관계 초기화
            post.getPostTags().clear();
        } else {
            // [CASE: 신규] ID가 없으면 새로 생성
            post = dto.toEntity(category, board, writer);
            post = postRepository.save(post);

            // 거래 게시글
            if ("trade".equalsIgnoreCase(category.getCode())) {
                if (dto.getTradeType() == null) {
                    throw new IllegalArgumentException("거래 유형은 필수입니다.");
                }
                post.updateTrade(dto.getTradeType(), TradeStatus.SELLING);
            }
            // 나눔 게시글
            if ("share".equalsIgnoreCase(category.getCode())) {
                post.updateMarketStatus(MarketStatus.SHARED);
            }
        }

        // 태그
        List<String> tags = dto.getTags() != null ? dto.getTags() : List.of();
        for (String tagName : tags) {
            if (tagName == null) continue;
            String cleanTag = tagName.trim();
            if (cleanTag.isEmpty()) continue;
            if (cleanTag.length() > 5) {
                cleanTag = cleanTag.substring(0, 5);
            }
            String finalTagName = cleanTag;
            Tag tag = tagRepository.findByName(cleanTag)
                    .orElseGet(() -> tagRepository.save(
                            Tag.builder()
                                    .name(finalTagName)
                                    .build()
                    ));
            PostTag postTag = PostTag.builder()
                    .post(post)
                    .tag(tag)
                    .build();
            post.addPostTag(postTag);
        }

        // 대표 썸네일 저장
        if (dto.getThumbnailFile() != null && !dto.getThumbnailFile().isEmpty()) {
            try {
                String storedName = fileService.storeFile(dto.getThumbnailFile());
                PostFile thumbnail = PostFile.builder()
                        .post(post)
                        .originalName(dto.getThumbnailFile().getOriginalFilename())
                        .storedName(storedName)
                        .filePath("/uploads/" + storedName)
                        .fileType(PostFileType.THUMBNAIL)
                        .fileSize(dto.getThumbnailFile().getSize())
                        .build();
                postFileRepository.save(thumbnail);
            } catch (IOException e) {
                throw new RuntimeException("썸네일 저장 중 오류가 발생했습니다.", e);
            }
        }

        // 일반 첨부 파일 저장
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (MultipartFile file : dto.getFiles()) {
                if (file == null || file.isEmpty()) continue;

                try {
                    String storedName = fileService.storeFile(file);
                    PostFile postFile = PostFile.builder()
                            .post(post)
                            .originalName(file.getOriginalFilename())
                            .storedName(storedName)
                            .filePath("/uploads/" + storedName)
                            .fileType(PostFileType.IMAGE)
                            .fileSize(file.getSize())
                            .build();
                    postFileRepository.save(postFile);
                } catch (IOException e) {
                    throw new RuntimeException("첨부파일 저장 중 오류가 발생했습니다.", e);
                }
            }
        }
        return post.getId();
    }

    // Read
    public Page<PostListResponse> searchPosts(Long boardId, Long categoryId, String searchType, String keyword, int page) {
        PostStatus status = PostStatus.ACTIVE;
        Pageable pageable = PageRequest.of(page, 15);

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
    public int getTempPostCount(String boardCode, Long writerId) {
        return postRepository.countByBoardCodeAndPostStatusAndWriterId(boardCode, PostStatus.DRAFT, writerId);
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

    // 임시저장
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
        return tagRepository.findTop10TagNamesByBoardId(boardId, PageRequest.of(0, 10));
    }

    public List<String> getTagsByPostId(Long postId) {
        return postRepository.findTagsByPostId(postId);
    }

    public List<String> searchTags(String keyword) {
        if (keyword == null || keyword.length() < 1) return List.of();
        return tagRepository.findTop5ByNameStartingWith(keyword, PageRequest.of(0, 5));
    }

    // 사용자 연동
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
