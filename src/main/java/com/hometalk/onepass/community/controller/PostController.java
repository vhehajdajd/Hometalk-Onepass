package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.*;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {

    private final PostService postService;
    private final BoardService boardService;
    private final CategoryService categoryService;
    private final CommentService commentService;
    private final ReportService reportService;
    private final FileService fileService;

    // 게시판
    @GetMapping("/{boardCode}")
    public String boardMain(@PathVariable String boardCode) {
        return "redirect:/community/" + boardCode + "/all";
    }

    // 카테고리
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}")
    public String categoryList(@PathVariable String boardCode,
                               @PathVariable String categoryCode,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(required = false) String searchType,
                               @RequestParam(required = false) String keyword,
                               Model model,
                               Authentication authentication) {

        BoardResponseDTO board = boardService.findByCode(boardCode);
        CategoryResponseDTO category = "all".equalsIgnoreCase(categoryCode)
                ? null
                : categoryService.findByCode(categoryCode);

        int pageIndex = (page < 1) ? 0 : page - 1;

        return fillCommunityModel(board, category, pageIndex, searchType, keyword, model, authentication);
    }

    // 게시글 작성 폼
    @GetMapping("/{boardCode}/write")
    public String postForm(@PathVariable String boardCode,
                           Model model,
                           Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, true, authentication);
        model.addAttribute("post", new PostRequestDTO());

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        int tempCount = postService.getTempPostCount(boardCode, userDetails.getUserId());
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

    // 게시글 수정 폼
    @GetMapping("/{boardCode}/edit/{id}")
    public String postEditForm(@PathVariable String boardCode,
                               @PathVariable Long id,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, true, authentication);

        try {
            PostRequestDTO post = postService.getPostForEdit(id, boardCode);
            model.addAttribute("post", post);
            model.addAttribute("postId", id);
        } catch (PostNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않거나 삭제된 게시글입니다.");
            return "redirect:/community/square/all";
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        int tempCount = postService.getTempPostCount(boardCode, userDetails.getUserId());
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

    // 게시글 저장
    @PostMapping("/{boardCode}/save")
    public String createPost(@PathVariable String boardCode,
                             @ModelAttribute PostRequestDTO dto,
                             @RequestParam(name = "isTemp", defaultValue = "false") boolean isTemp,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        dto.setPostStatus(isTemp ? PostStatus.DRAFT : PostStatus.ACTIVE);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long id = postService.postSave(boardCode, dto, userDetails.getUserId());

        String msg = isTemp ? "게시글이 임시저장되었습니다." : "글이 성공적으로 등록되었습니다.";
        redirectAttributes.addFlashAttribute("message", msg);

        if (isTemp) {
            return "redirect:/community/" + boardCode + "/edit/" + id;
        }

        return "redirect:/community/" + boardCode + "/all/" + id;
    }

    // 게시글 수정
    @PostMapping("/{boardCode}/edit/{id}")
    public String updatePost(@PathVariable String boardCode,
                             @PathVariable Long id,
                             @ModelAttribute PostRequestDTO dto,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        dto.setId(id);
        boolean isDraft = dto.getPostStatus() == PostStatus.DRAFT;
        if (dto.getPostStatus() == null || dto.getPostStatus() == PostStatus.DRAFT) {
            dto.setPostStatus(PostStatus.ACTIVE);
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String categoryPath = StringUtils.hasText(dto.getCategoryCode())
                ? dto.getCategoryCode()
                : "all";

        postService.postSave(boardCode, dto, userDetails.getUserId());

        redirectAttributes.addFlashAttribute(
                "message",
                isDraft ? "글이 성공적으로 등록되었습니다." : "게시글이 수정되었습니다."
        );
        return "redirect:/community/" + boardCode + "/" + categoryPath + "/" + id;
    }

    // 게시글 삭제
    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode,
                             @PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        postService.deletePost(id, userDetails.getUserId(), boardCode);

        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/community/" + boardCode + "/all";
    }

    // 게시글 상세 페이지
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode,
                             @PathVariable String categoryCode,
                             @PathVariable Long id,
                             @RequestParam(value = "fromReport", required = false) Boolean fromReport,
                             @RequestParam(value = "page", defaultValue = "1") int page,
                             HttpSession session,
                             Model model,
                             Authentication authentication) {

        PostUserRsDTO currentUser = getPostUser(authentication);

        List<Long> viewedPosts = (List<Long>) session.getAttribute("viewedPosts");
        if (viewedPosts == null) {
            viewedPosts = new ArrayList<>();
            session.setAttribute("viewedPosts", viewedPosts);
        }

        PostResponseDTO post = postService.postDetail(id, currentUser, boardCode, viewedPosts);
        model.addAttribute("post", post);

        CategoryResponseDTO category;
        if ("all".equals(categoryCode)) {
            category = categoryService.findById(post.getCategoryId(), boardCode);
        } else {
            category = categoryService.findByCode(categoryCode);
        }

        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, category, model, false, authentication);

        if (Boolean.TRUE.equals(fromReport)
                && authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"))) {
            reportService.markReviewing(id);
        }

        model.addAttribute("boardCode", boardCode);
        model.addAttribute("currentCategoryCode", categoryCode);
        model.addAttribute("currentPage", page);
        model.addAttribute("comments", commentService.findAllByPostId(id));
        model.addAttribute("postTags", postService.getTagsByPostId(id));

        return "community/postDetail";
    }

    private void addLayoutAttributes(BoardResponseDTO board,
                                     CategoryResponseDTO category,
                                     Model model,
                                     boolean isWriteMode,
                                     Authentication authentication) {
        if (board == null) return;
        model.addAttribute("board", board);
        model.addAttribute("category", category);
        model.addAttribute("boards", boardService.findAll());
        model.addAttribute("loginUser", getPostUser(authentication));

        List<CategoryResponseDTO> categories = isWriteMode
                ? categoryService.findAllByBoardIdForWrite(board.getId())
                : categoryService.findAllByBoardId(board.getId());

        model.addAttribute("categories", categories);
        model.addAttribute("boardId", board.getId());
        model.addAttribute("categoryId", category != null ? category.getId() : null);
        model.addAttribute("currentBoardCode", board.getCode());
    }

    private String fillCommunityModel(BoardResponseDTO board,
                                      CategoryResponseDTO category,
                                      int page,
                                      String searchType,
                                      String keyword,
                                      Model model,
                                      Authentication authentication) {
        if (board == null) return "redirect:/community";

        if (category == null) {
            model.addAttribute("categoryCode", "all");
            model.addAttribute("categoryId", null);
        } else {
            model.addAttribute("categoryCode", category.getCode());
            model.addAttribute("categoryId", category.getId());
        }

        addLayoutAttributes(board, category, model, false, authentication);

        if (StringUtils.hasText(keyword) && !StringUtils.hasText(searchType)) {
            model.addAttribute("searchError", "검색 유형을 선택해주세요.");
        }

        CustomUserDetails loginUser = null;
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails) {
            loginUser = (CustomUserDetails) authentication.getPrincipal();
        }

        Page<PostListResponse> postsPage = postService.searchPosts(
                board.getId(),
                category != null ? category.getId() : null,
                searchType,
                keyword,
                page,
                loginUser
        );

        model.addAttribute("posts", postsPage.getContent());
        model.addAttribute("page", postsPage);
        model.addAttribute("currentPage", page + 1);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("boardTags", postService.getTagsByBoardId(board.getId()));

        return "community/postList";
    }

    private PostUserRsDTO getPostUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return PostUserRsDTO.builder()
                .userId(userDetails.getUserId())
                .nickname(userDetails.getNickname())
                .role(String.valueOf(userDetails.getRole()))
                .build();
    }

}