package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.*;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.service.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

import java.io.IOException;
import java.util.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/community")
public class PostController {

    private final PostService postService;
    private final BoardService boardService;
    private final CategoryService categoryService;
    private final CommentService commentService;
    private final FileService fileService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${file.upload.path}")
    private String uploadPath;

    @GetMapping("/{boardCode}")
    public String boardMain(@PathVariable String boardCode) {
        return "redirect:/community/" + boardCode + "/all";
    }

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
        Long userId = getLoginUserId(authentication);
        int tempCount = postService.getTempPostCount(boardCode, userId);
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

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
        Long userId = getLoginUserId(authentication);
        int tempCount = postService.getTempPostCount(boardCode, userId);
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

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

        Long userId = getLoginUserId(authentication);
        Long id = postService.postSave(boardCode, dto, userId);

        String msg = isTemp ? "게시글이 임시저장되었습니다." : "글이 성공적으로 등록되었습니다.";
        redirectAttributes.addFlashAttribute("message", msg);

        if (isTemp) {
            return "redirect:/community/" + boardCode + "/edit/" + id;
        }

        return "redirect:/community/" + boardCode + "/all/" + id;
    }

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

        Long userId = getLoginUserId(authentication);

        String categoryPath = StringUtils.hasText(dto.getCategoryCode())
                ? dto.getCategoryCode()
                : "all";

        postService.postSave(boardCode, dto, userId);

        redirectAttributes.addFlashAttribute(
                "message",
                isDraft ? "글이 성공적으로 등록되었습니다." : "게시글이 수정되었습니다."
        );
        return "redirect:/community/" + boardCode + "/" + categoryPath + "/" + id;
    }

    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode,
                             @PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        Long userId = getLoginUserId(authentication);
        postService.deletePost(id, userId, boardCode);

        redirectAttributes.addFlashAttribute("message", "게시글이 삭제되었습니다.");
        return "redirect:/community/" + boardCode + "/all";
    }


    @PostMapping("/{boardCode}/save-temp")
    @ResponseBody
    public ResponseEntity<?> saveTempApi(@PathVariable String boardCode,
                                         @ModelAttribute PostRequestDTO dto,
                                         Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        dto.setPostStatus(PostStatus.DRAFT);

        Long userId = getLoginUserId(authentication);

        Long id;
        if (dto.getId() != null) {
            id = postService.postSave(boardCode, dto, userId); // 기존 id 있으면 update 되게
        } else {
            id = postService.postSave(boardCode, dto, userId); // id 없으면 insert
        }

        return ResponseEntity.ok(Map.of(
                "id", id,
                "message", "게시글이 임시저장되었습니다."
        ));
    }

    @GetMapping("/{boardCode}/temp-count")
    @ResponseBody
    public int getTempCount(@PathVariable String boardCode, Authentication authentication) {
        if (authentication == null) return 0;
        Long userId = getLoginUserId(authentication);
        return postService.getTempPostCount(boardCode, userId);
    }

    @GetMapping("/{boardCode}/temp-list")
    @ResponseBody
    public List<PostListResponse> getTempPosts(@PathVariable String boardCode,
                                               RedirectAttributes redirectAttributes,
                                               Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        }

        Long userId = getLoginUserId(authentication);
        redirectAttributes.addFlashAttribute("message", "임시저장 되었습니다.");
        return postService.getTempPosts(boardCode, userId);
    }

    @PostMapping("/{boardCode}/delete-temp/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteTemp(@PathVariable String boardCode,
                                             @PathVariable Long id,
                                             Authentication authentication) {

        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
            }

            Long userId = getLoginUserId(authentication);
            postService.deletePost(id, userId, boardCode);

            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail");
        }
    }

    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode,
                             @PathVariable String categoryCode,
                             @PathVariable Long id,
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

        model.addAttribute("boardCode", boardCode);
        model.addAttribute("currentCategoryCode", categoryCode);

        model.addAttribute("currentPage", page);

        model.addAttribute("comments", commentService.findAllByPostId(id));
        model.addAttribute("postTags", postService.getTagsByPostId(id));

        return "community/postDetail";
    }

    @PostMapping("/image-upload")
    @ResponseBody
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String storeFileName = fileService.storeFile(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", "/uploads/" + storeFileName);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
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

        if (board == null) {
            return "redirect:/community";
        }

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

    private Long getLoginUserId(Authentication authentication) {
        User user = getLoginUser(authentication);
        return user.getId();
    }

    private PostUserRsDTO getPostUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        User user = getLoginUser(authentication);

        return PostUserRsDTO.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .build();
    }

    private User getLoginUser(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Object principal = authentication.getPrincipal();

        // 1순위: CustomUserDetails 안에 userId가 있으면 바로 User 조회
        if (principal instanceof CustomUserDetails customUserDetails) {
            Long userId = customUserDetails.getUserId();

            if (userId != null) {
                User user = entityManager.find(User.class, userId);

                if (user != null) {
                    return user;
                }
            }
        }

        // 2순위: userId가 없으면 loginId로 조회
        String loginId = authentication.getName();

        List<User> users = entityManager.createQuery(
                        "select u " +
                                "from LocalAccount la " +
                                "join la.user u " +
                                "where la.loginId = :loginId",
                        User.class
                )
                .setParameter("loginId", loginId)
                .setMaxResults(1)
                .getResultList();

        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }

        return users.get(0);
    }
}