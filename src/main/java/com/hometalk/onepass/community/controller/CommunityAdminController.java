package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.AdminBoardRqDTO;
import com.hometalk.onepass.community.dto.AdminBoardRsDTO;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.entity.Post;
import com.hometalk.onepass.community.enums.PostStatus;
import com.hometalk.onepass.community.repository.PostRepository;
import com.hometalk.onepass.community.service.BoardService;
import com.hometalk.onepass.community.service.CommunityAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/community/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CommunityAdminController {
    private final CommunityAdminService communityAdminService;

    // 게시판&카테고리 목록 조회
    @GetMapping
    public String adminPage(Model model) {
        List<AdminBoardRsDTO> boards = communityAdminService.getAdminBoardList();
        model.addAttribute("boards", boards);
        model.addAttribute("adminBoardRqDTO", new AdminBoardRqDTO());
        return "community/admin-management";
    }

    // 상세 조회
    @GetMapping("/board/detail/{id}")
    public String getBoardDetail(@PathVariable Long id, Model model) {
        AdminBoardRsDTO boardDetail = communityAdminService.getAdminBoardDetail(id);
        model.addAttribute("board", boardDetail);
        return "community/board_detail"; // 상세 페이지 뷰 이름
    }

    // 게시판 생성 (카테고리 포함)
    @PostMapping("/board/create")
    public String createBoard(@ModelAttribute AdminBoardRqDTO adminBoardRqDTO) {
        communityAdminService.createBoard(adminBoardRqDTO);
        return "redirect:/community/admin";
    }

    // 게시판 삭제
    @PostMapping("/board/delete/{id}")
    public String deleteBoard(@PathVariable Long id, RedirectAttributes rttr) {
        try {
            communityAdminService.deleteBoard(id);
            rttr.addFlashAttribute("message", "게시판이 성공적으로 삭제되었습니다.");
        } catch (IllegalStateException e) {
            rttr.addFlashAttribute("errorMessage", e.getMessage());
            System.out.println("삭제 에러 발생: " + e.getMessage());
        }
        return "redirect:/community/admin";
    }

    // 숨김/삭제 게시글 관리 페이지
    @GetMapping("/posts")
    public String managedPostsPage(Model model) {
        List<PostResponseDTO> posts = communityAdminService.getAdminManagedPosts();
        model.addAttribute("posts", posts);
        return "community/admin-posts"; // 별도의 관리 페이지 뷰
    }

    // 카테고리 생성
    @PostMapping("/category/create")
    public String createCategory(@RequestParam Long boardId,
                                 @RequestParam String name,
                                 @RequestParam String code,
                                 @RequestParam String color,
                                 RedirectAttributes redirectAttributes) {
        try {
            // 서비스 호출 (수정된 파라미터 포함)
            communityAdminService.addCategory(boardId, name, code, color);
            redirectAttributes.addFlashAttribute("message", "카테고리가 성공적으로 추가되었습니다.");
        } catch (IllegalStateException e) {
            // "카테고리는 최대 5개까지만..." 등의 메시지를 화면으로 전달
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
        }
        return "redirect:/community/admin/board/detail/" + boardId;
    }

    // 카테고리 이름 수정 (AJAX로 처리할 경우 @ResponseBody 사용 가능)
    @GetMapping("/category/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam("name") String newName,
                                 @RequestParam Long boardId) {
        communityAdminService.updateCategory(id, newName);
        return "redirect:/community/admin/board/detail/" + boardId;
    }

    // 6. 카테고리 삭제
    @GetMapping("/category/delete/{id}")
    public String deleteCategory(@PathVariable Long id, @RequestParam Long boardId,
                                 RedirectAttributes rttr) {
        try {
            communityAdminService.deleteCategory(id);
            rttr.addFlashAttribute("message", "카테고리가 삭제되었습니다.");
        } catch (IllegalStateException e) {
            rttr.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            rttr.addFlashAttribute("errorMessage", "삭제 중 알 수 없는 오류가 발생했습니다.");
        }
        return "redirect:/community/admin/board/detail/" + boardId;
    }

    // 영구 삭제 처리
    @PostMapping("/posts/hard-delete/{id}")
    public String hardDeletePost(@PathVariable Long id, RedirectAttributes rttr) {
        try {
            communityAdminService.hardDeletePost(id);
            rttr.addFlashAttribute("message", "게시글이 DB에서 영구 삭제되었습니다.");
        } catch (Exception e) {
            rttr.addFlashAttribute("errorMessage", "삭제 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/community/admin/posts";
    }

    @PostMapping("/posts/hard-delete/batch")
    public String hardDeleteBatch(@RequestParam List<Long> postIds, RedirectAttributes redirectAttributes) {
        try {
            communityAdminService.hardDeletePosts(postIds);
            redirectAttributes.addFlashAttribute("message", postIds.size() + "건이 영구 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "삭제 중 오류 발생: " + e.getMessage());
        }
        return "redirect:/community/admin/posts";
    }
}
