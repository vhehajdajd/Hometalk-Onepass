package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintPageController {

    private final ComplaintService complaintService;

    @GetMapping("/list")
    public String list(Model model,
                       Authentication authentication,
                       @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        Long userId = complaintService.getLoginUserId(authentication);
        boolean isAdmin = complaintService.isAdmin(authentication);

        Page<ComplaintDto> paging = complaintService.findAll(userId, isAdmin, pageable);

        model.addAttribute("paging", paging != null ? paging : Page.empty(pageable));
        return "inquiry/complaintList";
    }

    @GetMapping("/write")
    public String writePage(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        return "inquiry/complaintWrite";
    }

    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id,
                             Authentication authentication,
                             Model model) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/auth";
        }

        ComplaintDto complaintDto = complaintService.getComplaintDetail(id, authentication);

        model.addAttribute("complaint", complaintDto);
        return "inquiry/complaintDetail";
    }
}