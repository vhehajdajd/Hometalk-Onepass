package com.hometalk.onepass.auth.controller;

import com.hometalk.onepass.auth.service.AdminUserApprovalService;
import com.hometalk.onepass.auth.entity.User;
import lombok.RequiredArgsConstructor;
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
    public String approvals(Model model) {
        model.addAttribute("pendingUsers", adminUserApprovalService.getPendingUsers());
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
