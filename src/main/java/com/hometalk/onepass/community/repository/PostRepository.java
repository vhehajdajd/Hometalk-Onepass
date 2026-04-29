package com.hometalk.onepass.community.repository;

import com.hometalk.onepass.community.entity.Board;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    int countByBoardCodeAndPostStatus(String boardCode, PostStatus status);

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

    // 검색
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

    long countByCategoryId(Long categoryId);

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

    // 특정 상태(숨김, 삭제 등)인 게시글만 모아보기
    List<Post> findAllByPostStatusInOrderByCreatedAtDesc(List<PostStatus> statuses);

    // 삭제 상태이면서 업데이트 날짜(삭제 시점)가 특정 날짜 이전인 데이터 조회
    List<Post> findAllByPostStatusAndUpdatedAtBefore(PostStatus status, LocalDateTime dateTime);


    // 최신순 상위 3개
    List<Post> findTop3ByPostStatusOrderByCreatedAtDesc(PostStatus status);

    // 조회수 정렬
    List<Post> findTop5ByPostStatusOrderByViewCountDesc(PostStatus status);
}
