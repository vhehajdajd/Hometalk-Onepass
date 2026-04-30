package com.hometalk.onepass.community.controller;

import com.hometalk.onepass.community.dto.AdminBoardRqDTO;
import com.hometalk.onepass.community.dto.AdminBoardRsDTO;
import com.hometalk.onepass.community.service.BoardService;
import com.hometalk.onepass.community.service.CommunityAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @PostMapping("/create")
    public String createBoard(AdminBoardRqDTO adminBoardRqDTO) {
        communityAdminService.createBoardWithCategories(adminBoardRqDTO);
        return "redirect:/community/admin";
    }
}
