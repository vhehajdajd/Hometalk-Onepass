package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.dto.AdminUserApprovalResponseDTO;
import com.hometalk.onepass.auth.service.AdminUserApprovalService;
import com.hometalk.onepass.auth.entity.User;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/users")
public class AdminUserApprovalController {

    private final AdminUserApprovalService adminUserApprovalService;

    @GetMapping("/approvals")
    public String approvals(@RequestParam(defaultValue = "0") int page, Model model) {
        int safePage = Math.max(page, 0);
        Sort requestDateSort = Sort.by(Sort.Direction.DESC, "updatedAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Page<AdminUserApprovalResponseDTO> pendingUsers = adminUserApprovalService.getPendingUsers(
                PageRequest.of(safePage, 15, requestDateSort)
        );
        if (pendingUsers.getTotalPages() > 0 && safePage >= pendingUsers.getTotalPages()) {
            pendingUsers = adminUserApprovalService.getPendingUsers(
                    PageRequest.of(pendingUsers.getTotalPages() - 1, 15, requestDateSort)
            );
        }
        int currentPage = pendingUsers.getNumber();
        int totalPages = pendingUsers.getTotalPages();
        int pageGroupStart = totalPages == 0 ? 0 : (currentPage / 10) * 10;
        int pageGroupEnd = totalPages == 0 ? 0 : Math.min(pageGroupStart + 9, totalPages - 1);

        model.addAttribute("pendingUsers", pendingUsers.getContent());
        model.addAttribute("pendingUserPage", pendingUsers);
        model.addAttribute("pageGroupStart", pageGroupStart);
        model.addAttribute("pageGroupEnd", pageGroupEnd);
        return "auth/admin/user-approvals";
    }

    @PostMapping("/{userId}/approve")
    public String approve(@PathVariable Long userId,
                          @RequestParam User.UserRole role) {
        adminUserApprovalService.approve(userId, role);
        return "redirect:/admin/users/approvals";
    }

    @PostMapping("/{userId}/reject")
    public String reject(@PathVariable Long userId) {
        adminUserApprovalService.reject(userId);
        return "redirect:/admin/users/approvals";
    }
}
