package com.hometalk.onepass.community.service;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.auth.repository.UserRepository;
import com.hometalk.onepass.community.dto.CommunityPostResponseDTO;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.PostListResponse;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.dto.response.PostUserRsDTO;
import com.hometalk.onepass.community.dto.response.ReactionStatus;
import com.hometalk.onepass.community.entity.*;
import com.hometalk.onepass.community.enums.*;
import com.hometalk.onepass.community.exception.*;
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
import java.util.*;
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
    private final PostReactionRepository postReactionRepository;

    // Create
    @Transactional
    public Long postSave(String boardCode, PostRequestDTO dto, Long userId) {
        Board board = boardRepository.findByCode(boardCode)
                .orElseThrow(() -> new InvalidBoardCodeException(boardCode));
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(dto.getCategoryId(), boardCode));
        }
        // 작성자 정보 조회
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId, boardCode));

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
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                throw new EmptyContentException("제목은 필수 입력 항목입니다.", boardCode);
            }
            if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
                throw new EmptyContentException("내용은 필수 입력 항목입니다.", boardCode);
            }

            post = dto.toEntity(category, board, writer);
            post = postRepository.save(post);

            if (category != null) {
                // 거래 게시글
                if ("trade".equalsIgnoreCase(category.getCode())) {
                    if (dto.getTradeType() == null) {
                        throw new EmptyContentException("거래 유형(TradeType)은 필수입니다.", boardCode);
                    }
                    post.updateTrade(dto.getTradeType(), TradeStatus.SELLING);
                }
                // 나눔 게시글
                if ("share".equalsIgnoreCase(category.getCode())) {
                    post.updateMarketStatus(MarketStatus.SHARED);
                }
            }
        }

        // 태그
        List<String> tags = dto.getTags() != null ? dto.getTags() : List.of();
        if (!tags.isEmpty()) {
            List<String> cleanTagNames = tags.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(name -> name.length() > 5 ? name.substring(0, 5) : name)
                    .distinct()
                    .toList();

            if (!cleanTagNames.isEmpty()) {
                List<Tag> existingTags = tagRepository.findByNameIn(cleanTagNames);
                Set<String> existingTagNames = existingTags.stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet());

                List<Tag> newTags = cleanTagNames.stream()
                        .filter(name -> !existingTagNames.contains(name))
                        .map(name -> Tag.builder().name(name).build())
                        .toList();

                if (!newTags.isEmpty()) {
                    tagRepository.saveAll(newTags); // 일괄 저장
                }

                List<Tag> allTags = new ArrayList<>();
                allTags.addAll(existingTags);
                allTags.addAll(newTags);

                for (Tag tag : allTags) {
                    PostTag postTag = PostTag.builder()
                            .post(post)
                            .tag(tag)
                            .build();
                    post.addPostTag(postTag);
                }
            }
        }

        // 대표 썸네일 저장
        if (dto.getThumbnailFile() != null && !dto.getThumbnailFile().isEmpty()) {
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
        }

        // 일반 첨부 파일 저장
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (MultipartFile file : dto.getFiles()) {
                if (file == null || file.isEmpty()) continue;

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
            }
        }
        return post.getId();
    }

    // Read
    public Page<PostListResponse> searchPosts(Long boardId, Long categoryId, String searchType, String keyword, int page,
                                              CustomUserDetails loginUser) {
        PostStatus status = PostStatus.ACTIVE;
        Pageable pageable = PageRequest.of(page, 15);

        // 게시판 조회
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new InvalidBoardCodeException(String.valueOf(boardId)));

        Page<Post> posts;
        if (keyword == null || keyword.isBlank()) {
            posts = getNormalList(boardId, categoryId, status, pageable);
        } else {
            posts = switch (searchType) {
                case "title" -> postRepository.findByTitle(board, keyword, status, pageable);
                case "nickname" -> postRepository.findByNickname(board, keyword, status, pageable);
                case "tc" -> postRepository.findByTitleOrContent(board, keyword, status, pageable);
                case "tag" -> postRepository.findByTagName(board.getId(), keyword, status, pageable);
                default -> getNormalList(boardId, categoryId, status, pageable);
            };
        }
        User currentUser = (loginUser != null)
                ? userRepository.findById(loginUser.getUserId()).orElse(null)
                : null;

        return posts.map(post -> {
            boolean liked = false;
            boolean disliked = false;
            // N+1 제거
            if (currentUser != null) {
                liked = postReactionRepository.findByPostAndUserAndType(post, currentUser, ReactionType.LIKE).isPresent();
                disliked = postReactionRepository.findByPostAndUserAndType(post, currentUser, ReactionType.DISLIKE).isPresent();
            }
            return new PostListResponse(post, liked, disliked);
        });
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
        ReactionStatus status = postActionService.getReactionStatus(postId, currentUserId);

        PostResponseDTO dto = new PostResponseDTO(post);
        dto.setLiked(status.isLiked());
        dto.setDisliked(status.isDisliked());
        dto.setLikeCount(status.getLikeCount());
        dto.setDislikeCount(status.getDislikeCount());
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
        return tagRepository.findNameByNameStartingWith(keyword, PageRequest.of(0, 5));
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
