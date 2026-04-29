package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.AdminBoardRqDTO;
import com.hometalk.onepass.community.dto.AdminBoardRsDTO;
import com.hometalk.onepass.community.dto.response.PostResponseDTO;
import com.hometalk.onepass.community.service.BoardService;
import com.hometalk.onepass.community.service.CommunityAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/community/admin")
@RequiredArgsConstructor
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
    public String deleteBoard(@PathVariable Long id) {
        communityAdminService.deleteBoard(id);
        return "redirect:/community/admin";
    }

    // 숨김/삭제 게시글 관리 페이지
    @GetMapping("/posts")
    public String managedPostsPage(Model model) {
        List<PostResponseDTO> managedPosts = communityAdminService.getAdminManagedPosts();
        model.addAttribute("posts", managedPosts);
        return "community/admin-posts"; // 별도의 관리 페이지 뷰
    }

    // 카테고리 생성
    @PostMapping("/category/create")
    public String createCategory(@RequestParam Long boardId, @RequestParam String name) {
        communityAdminService.addCategory(boardId, name);
        return "redirect:/community/admin/board/detail/" + boardId;
    }

    // 카테고리 이름 수정 (AJAX로 처리할 경우 @ResponseBody 사용 가능)
    @PostMapping("/category/update/{id}")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam("name") String newName,
                                 @RequestParam Long boardId) {
        communityAdminService.updateCategory(id, newName);
        return "redirect:/community/admin/board/detail/" + boardId;
    }

    // 6. 카테고리 삭제
    @PostMapping("/category/delete/{id}")
    public String deleteCategory(@PathVariable Long id, @RequestParam Long boardId) {
        communityAdminService.deleteCategory(id);
        return "redirect:/community/admin/board/detail/" + boardId;
    }
}
