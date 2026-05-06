package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    int countByBoardCodeAndPostStatus(String boardCode, PostStatus status);


    // --- [사용자용 조회] : @SQLRestriction ("deleted_at IS NULL") 자동 적용 ---
    @Query("SELECT p FROM Post p WHERE p.board.code = :boardCode " +
            "AND p.writer.id = :writerId AND p.postStatus = :status ORDER BY p.id DESC")
    List<Post> findTempPosts(String boardCode, Long writerId, PostStatus status);

    // 임시저장은 목록 숨기기
    // 게시판 전체 글 조회
    @EntityGraph(attributePaths = {"category", "board", "writer", "postTags.tag"})
    @Query("SELECT p FROM Post p WHERE p.board.id = :boardId AND p.postStatus = :status")
    Page<Post> findActivePosts(@Param("boardId") Long boardId,
                               @Param("status") PostStatus status,
                               Pageable pageable);

    // 특정 게시판 내 특정 카테고리 글 조회
    @EntityGraph(attributePaths = {"category", "board", "writer", "postTags.tag"})
    @Query("SELECT p FROM Post p WHERE p.board.id = :boardId AND p.category.id = :catId AND p.postStatus = :status")
    Page<Post> findCategoryPosts(@Param("boardId") Long boardId,
                                 @Param("catId") Long catId,
                                 @Param("status") PostStatus status,
                                 Pageable pageable);

    long countByCategoryId(Long categoryId);
    long countByBoardId(Long boardId);

    // -- 검색 --
    // 제목
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
            "AND p.postStatus = :status " +
            "AND p.title LIKE %:keyword%")
    Page<Post> findByTitle(@Param("board") Board board,
                           @Param("keyword") String keyword,
                           @Param("status") PostStatus status,
                           Pageable pageable);

    // 닉네임
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
            "AND p.postStatus = :status " +
            "AND p.writer.nickname LIKE %:keyword%")
    Page<Post> findByNickname(@Param("board") Board board,
                              @Param("keyword") String keyword,
                              @Param("status") PostStatus status,
                              Pageable pageable);

    // 제목 + 내용
    @Query("SELECT p FROM Post p WHERE p.board = :board " +
            "AND p.postStatus = :status " +
            "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> findByTitleOrContent(@Param("board") Board board,
                                    @Param("keyword") String keyword,
                                    @Param("status") PostStatus status,
                                    Pageable pageable);


    // 태그 검색 쿼리
    @EntityGraph(attributePaths = {"category", "board", "writer", "postTags.tag"})
    @Query("SELECT p FROM Post p " +
            "JOIN p.postTags pt " +
            "JOIN pt.tag t " +
            "WHERE p.board.id = :boardId AND t.name = :tagName AND p.postStatus = :status")
    Page<Post> findByTagName(@Param("boardId") Long boardId,
                             @Param("tagName") String tagName,
                             @Param("status") PostStatus status,
                             Pageable pageable);

    @Query("SELECT t.name FROM PostTag pt JOIN pt.tag t WHERE pt.post.id = :postId")
    List<String> findTagsByPostId(@Param("postId") Long postId);


    // --- [관리자용 조회] : Soft Delete를 무시해야 하므로 Native Query 사용 ---
    // 1. 관리자 페이지 목록용 (삭제됨/숨김 상태의 글을 최신 수정일 순으로)
    @Query(value = "SELECT * FROM posts WHERE post_status IN (:statuses) ORDER BY updated_at DESC", nativeQuery = true)
    List<Post> findAllManagedPostsNative(@Param("statuses") List<String> statuses);

    // 2. 카테고리/게시판 삭제 전 체크용 (상태 상관없이 무조건 카운트)
    @Query(value = "SELECT COUNT(*) FROM posts WHERE category_id = :categoryId", nativeQuery = true)
    long countAllByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT COUNT(*) FROM posts WHERE board_id = :boardId", nativeQuery = true)
    long countAllByBoardIdNative(@Param("boardId") Long boardId);


    // --- [관리자용 영구 삭제 (Hard Delete)] ---
    // 0. 게시글-태그 관계를 먼저 영구 삭제 (제약 조건 해결용)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM post_tags WHERE post_id = :postId", nativeQuery = true)
    void hardDeletePostTagsByPostId(@Param("postId") Long postId);

    // 1. 댓글을 먼저 영구 삭제 (Native Query로 Soft Delete 우회)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM comments WHERE post_id = :postId", nativeQuery = true)
    void hardDeleteCommentsByPostId(@Param("postId") Long postId);

    // 2. 게시글을 영구 삭제 (Native Query로 @SQLDelete 우회)
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM posts WHERE id = :postId", nativeQuery = true)
    void hardDeletePostById(@Param("postId") Long postId);


    // -- API --
    // 최신순 상위 3개
    List<Post> findTop3ByPostStatusOrderByCreatedAtDesc(PostStatus status);

    // 조회수 정렬
    List<Post> findTop5ByPostStatusOrderByViewCountDesc(PostStatus status);



    List<Post> findAllByPostStatusInOrderByUpdatedAtDesc(List<PostStatus> targetStatuses);

    // --- [기타/스케줄러] ---
    // 스케줄러용: @SQLRestriction 때문에 Native Query 권장
    @Query(value = "SELECT * FROM posts WHERE post_status = :status AND updated_at < :dateTime", nativeQuery = true)
    List<Post> findOldDeletedPosts(@Param("status") String status, @Param("dateTime") LocalDateTime dateTime);

}
