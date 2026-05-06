package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller; // RestController가 아님!
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintPageController {

    private final ComplaintService complaintService;

    /*
     * 민원 리스트
     */
    @GetMapping("/list")
    public String list(Model model,
                       @AuthenticationPrincipal CustomUserDetails user,
                       @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        if (user == null) {
            return "redirect:/auth";
        }
        Page<ComplaintDto> paging = complaintService.findByUserId(user.getUserId(), pageable);

        model.addAttribute("paging", paging != null ? paging : Page.empty(pageable));
        return "inquiry/complaintList";
    }

    /*
     * 글쓰기 페이지
     */
    @GetMapping("/write")
    public String writePage() {
        return "inquiry/complaintWrite";
    }

    /*
     * 상세 페이지
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id,
                             @AuthenticationPrincipal CustomUserDetails user,
                             Model model) {

        ComplaintDto complaintDto =
                complaintService.getComplaintDetail(id, user);

        model.addAttribute("complaint", complaintDto);

        return "inquiry/complaintDetail";
    }
}