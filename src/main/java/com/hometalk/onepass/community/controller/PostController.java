package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.response.CommentRsDTO;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.*;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.service.BoardService;
import com.hometalk.onepass.community.service.CategoryService;
import com.hometalk.onepass.community.service.CommentService;
import com.hometalk.onepass.community.service.PostService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {
    private final PostService postService;
    private final BoardService boardService;
    private final CategoryService categoryService;
    private final CommentService commentService;

    // 게시판 목록
    // 게시판별 메인 (카테고리 '전체' 상태)
    @GetMapping("/{boardCode}")
    public String boardMain(@PathVariable String boardCode,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(required = false) String searchType,
                            @RequestParam(required = false) String keyword,
                            Model model) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        // 사용자의 첫 페이지(1)은 JPA에서 0으로 처리하므로 1씩 빼줘야 함
        int pageIndex = (page < 1) ? 0 : page - 1;
        return fillCommunityModel(board, null, pageIndex, searchType, keyword,  model);
    }

    // 카테고리별 목록
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}")
    public String categoryList(@PathVariable String boardCode,
                               @PathVariable String categoryCode,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(required = false) String searchType,
                               @RequestParam(required = false) String keyword,
                               Model model) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        CategoryResponseDTO category = "all".equals(categoryCode) ? null
                                        : categoryService.findByCode(categoryCode);
        int pageIndex = (page < 1) ? 0 : page - 1;

        return fillCommunityModel(board, category, pageIndex, searchType, keyword, model);
    }

    // 게시글 상세 페이지
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode,
                             @PathVariable String categoryCode,
                             @PathVariable Long id,
                             HttpSession session,
                             Model model) {
        // [임시] 아직 로그인 연동 전이므로 테스트용 유저 정보 직접 생성
        PostUserRsDTO tempUser = PostUserRsDTO.builder()
                .id(1L)           // 테스트하고 싶은 유저 ID
                .role("MEMBER")   // 또는 "ADMIN"
                .build();

        List<Long> viewedPosts = (List<Long>) session.getAttribute("viewedPosts");
        if (viewedPosts == null) {
            viewedPosts = new ArrayList<>();
            session.setAttribute("viewedPosts", viewedPosts);
        }

        // 1. 게시글 데이터 가져오기 (tempUser를 넘겨서 editable, admin 여부 계산)
        PostResponseDTO post = postService.postDetail(id, tempUser, boardCode, viewedPosts);
        model.addAttribute("post", post);

        // 2. 카테고리 배너 활성
        CategoryResponseDTO category;
        if ("all".equals(categoryCode)) {
            category = categoryService.findById(post.getCategoryId(), boardCode);
        } else {
            category = categoryService.findByCode(categoryCode);
        }

        // 3. 공통 레이아웃 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, category, model, false);
        model.addAttribute("boardCode", boardCode);
        model.addAttribute("currentCategoryCode", categoryCode);

        // 댓글
        List<CommentRsDTO> comments = commentService.findAllByPostId(id);
        model.addAttribute("comments", comments);
        return "community/postDetail";
    }

    // 게시글 작성 폼
    @GetMapping("/{boardCode}/write")
    public String postForm(@PathVariable String boardCode, Model model) {
        // 1. URL에서 받은 boardCode로 게시판 정보 조회
        BoardResponseDTO board = boardService.findByCode(boardCode);
        // 2. 공통 레이아웃(배너) 데이터
        addLayoutAttributes(board, null, model, true); // 배너와 헤더는 나오지만 목록은 안 가져옴
        // 3. 폼 입력을 위한 빈 DTO
        model.addAttribute("post", new PostRequestDTO());

        int tempCount = postService.getTempPostCount(boardCode);
        model.addAttribute("tempCount", tempCount);
        return "community/postForm";
    }

    // 게시글 수정 폼
    @GetMapping("/{boardCode}/edit/{id}")
    public String postForm(@PathVariable String boardCode,
                           @PathVariable Long id,
                           Model model) {
        // 공통 레이아웃(배너) 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, true); // 배너와 헤더는 나오지만 목록은 안 가져옴

        // ID가 있으면 - 임시저장 불러오기
        if (id != null) {
            PostRequestDTO post = postService.getPostForEdit(id, boardCode);
            model.addAttribute("post", post);
            model.addAttribute("postId", id);
        } else {
            model.addAttribute("post", new PostRequestDTO());
            model.addAttribute("postId", null);
        }
        int tempCount = postService.getTempPostCount(boardCode);
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

    // 게시글 등록
    @PostMapping("/{boardCode}/save")
    public String createPost(@PathVariable String boardCode, @ModelAttribute PostRequestDTO dto,
                             @RequestParam(name = "isTemp", defaultValue = "false") boolean isTemp,
                             RedirectAttributes redirectAttributes) {

        // 1. 임시저장 상태 설정
        dto.setPostStatus(isTemp ? PostStatus.DRAFT : PostStatus.ACTIVE);

        // [임시] 로그인 연동 전이므로 1번 유저로 고정
        Long tempUserId = 1L;

        // 2. 서비스 호출 및 저장
        Long id = postService.postSave(boardCode, dto, tempUserId);

        // 3. 상황에 맞는 성공 메시지 추가
        String msg = isTemp ? "게시글이 임시저장되었습니다." : "글이 성공적으로 등록되었습니다.";
        redirectAttributes.addFlashAttribute("successMessage", msg);

        // 4. 임시저장 여부에 따른 리다이렉트 분기
        if (isTemp) {
            return "redirect:/community/" + boardCode + "/write?id=" + id;
        }
        return "redirect:/community/" + boardCode + "/all/" + id;
    }

    // 게시글 수정
    @PostMapping("/{boardCode}/edit/{id}")
    public String updatePost(@PathVariable String boardCode, @PathVariable Long id, PostRequestDTO dto,
                             RedirectAttributes redirectAttributes) {
        // [임시] 수정 권한 테스트를 위한 고정 ID
        Long tempUserId = 1L;

        postService.postUpdate(id, dto, tempUserId, boardCode);
        redirectAttributes.addFlashAttribute("successMessage", "게시글이 수정되었습니다.");
        return "redirect:/community/" + boardCode + "/all/" + id;
    }

    // 게시글 삭제
    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode, @PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        // [임시] 테스트를 위해 1번 유저라고 가정
        Long tempUserId = 1L;

        postService.deletePost(id, tempUserId, boardCode);
        redirectAttributes.addFlashAttribute("successMessage", "게시글이 삭제되었습니다.");
        return "redirect:/community/" + boardCode + "/all";
    }

    @GetMapping("/{boardCode}/temp-list")
    @ResponseBody // JSON으로 반환
    public List<PostListResponse> getTempPosts(@PathVariable String boardCode) {
        Long tempUserId = 1L; // 테스트용 ID
        return postService.getTempPosts(boardCode, tempUserId);
    }


    // 공통 데이터 method
    private void addLayoutAttributes(BoardResponseDTO board, CategoryResponseDTO category,
                                     Model model, boolean isWriteMode) {
        if (board == null) return;

        model.addAttribute("board", board);
        model.addAttribute("category", category);
        model.addAttribute("boards", boardService.findAll()); // 게시판 헤더용

        // 글쓰기 모드일 때만 '전체'가 빠진 목록을 가져옴
        List<CategoryResponseDTO> categories;
        if (isWriteMode) {
            categories = categoryService.findAllByBoardIdForWrite(board.getId());
        } else {
            categories = categoryService.findAllByBoardId(board.getId());
        }
        model.addAttribute("categories", categories); // 카테고리 배너용
        model.addAttribute("boardId", board.getId());
        model.addAttribute("categoryId", (category != null) ? category.getId() : null);
    }

    // 공통 method - 배너 가져올 페이지/기능들에 모두 쓰임
    private String fillCommunityModel(BoardResponseDTO board,
                                      CategoryResponseDTO category,
                                      int page,
                                      String searchType,
                                      String keyword,
                                      Model model) {
        if (board == null) return "redirect:/community";    // 게시판 정보 없으면 메인 페이지

        if (category == null) {
            model.addAttribute("categoryCode", "all");
            model.addAttribute("categoryId", null);
        } else {
            model.addAttribute("categoryCode", category.getCode());
            model.addAttribute("categoryId", category.getId());
        }

        // 공통 레이아웃 데이터 채우기
        addLayoutAttributes(board, category, model, false);

        if (StringUtils.hasText(keyword) && !StringUtils.hasText(searchType)) {
            model.addAttribute("searchError", "검색 유형을 선택해주세요.");
        }
        // 목록 페이지 전용 데이터 채우기
        Page<PostListResponse> postsPage = postService.searchPosts(board.getId(),
                                           (category != null ? category.getId() : null),
                                           searchType, keyword,
                                           page);

        model.addAttribute("posts", postsPage.getContent());    // List<PostListReponse>
        model.addAttribute("page", postsPage);                  // 현재 페이지, 총 페이지 등
        model.addAttribute("currentPage", page + 1);  // 현재 페이지 번호

        // 검색 조건
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        return "community/postList";
    }
}
