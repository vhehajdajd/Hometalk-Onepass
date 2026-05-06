package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import com.hometalk.onepass.community.dto.response.CommentRsDTO;
import com.hometalk.onepass.community.dto.request.PostRequestDTO;
import com.hometalk.onepass.community.dto.response.*;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.exception.PostNotFoundException;
import com.hometalk.onepass.community.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
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

    // 게시판 목록
    // 게시판별 메인 (카테고리 '전체' 상태)
    @GetMapping("/{boardCode}")
    public String boardMain(@PathVariable String boardCode,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(required = false) String searchType,
                            @RequestParam(required = false) String keyword,
                            Model model,
                            @AuthenticationPrincipal CustomUserDetails user) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        // 사용자의 첫 페이지(1)은 JPA에서 0으로 처리하므로 1씩 빼줘야 함
        int pageIndex = (page < 1) ? 0 : page - 1;
        return fillCommunityModel(board, null, pageIndex, searchType, keyword,  model, user);
    }

    // 카테고리별 목록
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}")
    public String categoryList(@PathVariable String boardCode,
                               @PathVariable String categoryCode,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(required = false) String searchType,
                               @RequestParam(required = false) String keyword,
                               Model model,
                               @AuthenticationPrincipal CustomUserDetails user) {
        BoardResponseDTO board = boardService.findByCode(boardCode);
        CategoryResponseDTO category = "all".equals(categoryCode) ? null
                                        : categoryService.findByCode(categoryCode);
        int pageIndex = (page < 1) ? 0 : page - 1;

        return fillCommunityModel(board, category, pageIndex, searchType, keyword, model, user);
    }

    // 게시글 작성 폼
    @GetMapping("/{boardCode}/write")
    public String postForm(@PathVariable String boardCode, Model model,
                           @AuthenticationPrincipal CustomUserDetails user) {
        // 1. URL에서 받은 boardCode로 게시판 정보 조회
        BoardResponseDTO board = boardService.findByCode(boardCode);
        // 2. 공통 레이아웃(배너) 데이터
        addLayoutAttributes(board, null, model, true, user); // 배너와 헤더는 나오지만 목록은 안 가져옴
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
                           Model model,
                           RedirectAttributes redirectAttributes,
                           @AuthenticationPrincipal CustomUserDetails user) {
        // 공통 레이아웃(배너) 데이터
        BoardResponseDTO board = boardService.findByCode(boardCode);
        addLayoutAttributes(board, null, model, true, user); // 배너와 헤더는 나오지만 목록은 안 가져옴

        // ID가 있으면 - 임시저장 불러오기
        try {
            if (id != null) {
                PostRequestDTO post = postService.getPostForEdit(id, boardCode);
                model.addAttribute("post", post);
                model.addAttribute("postId", id);

                System.out.println("컨트롤러 로드 내용 확인: " + post.getContent());
            } else {
                model.addAttribute("post", new PostRequestDTO());
                model.addAttribute("postId", null);
            }
        } catch (PostNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "존재하지 않거나 삭제된 게시글입니다.");
            return "redirect:/hometop/community/square/all";
        }

        int tempCount = postService.getTempPostCount(boardCode);
        model.addAttribute("tempCount", tempCount);

        return "community/postForm";
    }

    // 게시글 등록
    @PostMapping("/{boardCode}/save")
    public String createPost(@PathVariable String boardCode, @ModelAttribute PostRequestDTO dto,
                             @RequestParam(name = "isTemp", defaultValue = "false") boolean isTemp,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal CustomUserDetails user) {

        // 1. 임시저장 상태 설정
        dto.setPostStatus(isTemp ? PostStatus.DRAFT : PostStatus.ACTIVE);

        if (user == null) {
            return "redirect:/auth"; // 또는 로그인 페이지
        }
        Long userId = user.getUserId();

        // 2. 서비스 호출 및 저장
        Long id = postService.postSave(boardCode, dto, userId);

        // 3. 상황에 맞는 성공 메시지 추가
        String msg = isTemp ? "게시글이 임시저장되었습니다." : "글이 성공적으로 등록되었습니다.";
        redirectAttributes.addFlashAttribute("successMessage", msg);

        // 4. 임시저장 여부에 따른 리다이렉트 분기
        if (isTemp) {
            return "redirect:/community/" + boardCode + "/edit/" + id;
        }
        return "redirect:/community/" + boardCode + "/all/" + id;
    }

    // 게시글 수정
    @PostMapping("/{boardCode}/edit/{id}")
    public String updatePost(@PathVariable String boardCode, @PathVariable Long id, PostRequestDTO dto,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal CustomUserDetails user) {
        dto.setId(id);
        if (dto.getPostStatus() == null) {
            dto.setPostStatus(PostStatus.ACTIVE);
        }

        if (user == null) {
            return "redirect:/auth";
        }
        Long userId = user.getUserId();

        String categoryPath = (dto.getCategoryCode() != null && !dto.getCategoryCode().isEmpty())
                ? dto.getCategoryCode() : "all";
        postService.postSave(boardCode, dto, userId);
        redirectAttributes.addFlashAttribute("successMessage", "게시글이 수정되었습니다.");
        return "redirect:/community/" + boardCode + "/" + categoryPath + "/" + id;
    }

    // 게시글 삭제
    @PostMapping("/{boardCode}/delete/{id}")
    public String deletePost(@PathVariable String boardCode, @PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return "redirect:/auth";
        }
        Long userId = user.getUserId();
        postService.deletePost(id, userId, boardCode);

        redirectAttributes.addFlashAttribute("successMessage", "게시글이 삭제되었습니다.");
        return "redirect:/community/" + boardCode + "/all";
    }

    // 임시저장
    @GetMapping("/{boardCode}/temp-list")
    @ResponseBody // JSON으로 반환
    public List<PostListResponse> getTempPosts(@PathVariable String boardCode,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
        }
        Long userId = user.getUserId();
        return postService.getTempPosts(boardCode, userId);
    }

    // 임시저장 글 삭제
    @PostMapping("/{boardCode}/delete-temp/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteTemp(@PathVariable String boardCode, @PathVariable Long id,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        try {
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요");
            }
            Long userId = user.getUserId();
            postService.deletePost(id, userId, boardCode);
            return ResponseEntity.ok("Success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Fail");
        }
    }


    // 게시글 상세 페이지
    @GetMapping("/{boardCode}/{categoryCode:[a-zA-Z]+}/{id:[0-9]+}")
    public String postDetail(@PathVariable String boardCode,
                             @PathVariable String categoryCode,
                             @PathVariable Long id,
                             HttpSession session,
                             Model model,
                             @AuthenticationPrincipal CustomUserDetails user) {

        PostUserRsDTO currentUser = (user != null) ? PostUserRsDTO.from(user) : null;

        List<Long> viewedPosts = (List<Long>) session.getAttribute("viewedPosts");
        if (viewedPosts == null) {
            viewedPosts = new ArrayList<>();
            session.setAttribute("viewedPosts", viewedPosts);
        }

        // 1. 게시글 데이터 가져오기 (tempUser를 넘겨서 editable, admin 여부를 계산함)
        PostResponseDTO post = postService.postDetail(id, currentUser, boardCode, viewedPosts);
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
        addLayoutAttributes(board, category, model, false, user);
        model.addAttribute("boardCode", boardCode);
        model.addAttribute("currentCategoryCode", categoryCode);

        // 댓글
        List<CommentRsDTO> comments = commentService.findAllByPostId(id);
        model.addAttribute("comments", comments);

        // 태그
        List<String> postTags = postService.getTagsByPostId(id);
        model.addAttribute("postTags", postTags);
        return "community/postDetail";
    }

    // 이미지
    @Value("${file.upload.path}")
    private String uploadPath;

    @PostMapping("/image-upload")
    @ResponseBody
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file,
                                           HttpServletRequest request) {
        try {
            // 1. 설정된 경로(uploadPath)가 없으면 생성
            File dir = new File(uploadPath);
            if (!dir.exists()) dir.mkdirs();

            // 2. 파일명 중복 방지 (UUID)
            String original = file.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + (original != null ? original : "image");
            File dest = new File(dir, fileName);

            // 3. 실제 폴더에 저장
            file.transferTo(dest.getAbsoluteFile());

            // 4. 브라우저가 접근할 URL 생성
            String contextPath = request.getContextPath(); // "/hometop"
            Map<String, String> result = new HashMap<>();
            result.put("url", contextPath + "/uploads/" + fileName);

            return result;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }


    // 공통 데이터 method
    private void addLayoutAttributes(BoardResponseDTO board,
                                     CategoryResponseDTO category,
                                     Model model,
                                     boolean isWriteMode,
                                     CustomUserDetails user) {
        if (board == null) return;

        model.addAttribute("board", board);
        model.addAttribute("category", category);
        model.addAttribute("boards", boardService.findAll()); // 게시판 헤더용

        model.addAttribute("loginUser",
                user != null ? PostUserRsDTO.from(user) : null);

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
        model.addAttribute("currentBoardCode", board.getCode());
    }

    // 공통 method - 배너 가져올 페이지/기능들에 모두 쓰임
    private String fillCommunityModel(BoardResponseDTO board,
                                      CategoryResponseDTO category,
                                      int page,
                                      String searchType,
                                      String keyword,
                                      Model model,
                                      CustomUserDetails user) {
        if (board == null) return "redirect:/community";    // 게시판 정보 없으면 메인 페이지

        if (category == null) {
            model.addAttribute("categoryCode", "all");
            model.addAttribute("categoryId", null);
        } else {
            model.addAttribute("categoryCode", category.getCode());
            model.addAttribute("categoryId", category.getId());
        }

        // 공통 레이아웃 데이터 채우기
        addLayoutAttributes(board, category, model, false, user);

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

        // 상단 태그 나열
        List<String> boardTags = postService.getTagsByBoardId(board.getId());
        model.addAttribute("boardTags", boardTags);
        return "community/postList";
    }
}
