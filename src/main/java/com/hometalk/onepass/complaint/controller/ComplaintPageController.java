package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.complaint.dto.ComplaintDto;
import com.hometalk.onepass.complaint.service.ComplaintService;
import com.hometalk.onepass.inquiry.dto.InquiryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/list")
    public String listPage(Model model,
                           @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ComplaintDto> paging = complaintService.findAll(pageable);
        // 1. 서비스에서 데이터를 가져와서
        model.addAttribute("paging", paging);
        // 2. templates/complaints/list.html 파일을 찾아서 띄워줌
        return "inquiry/complaintList";
    }

    // 2. 글쓰기 페이지
    @GetMapping("/write")
    public String writePage() {
        // templates/inquiry/complaintWrite.html 파일을 찾아서 보여줌
        return "inquiry/complaintWrite";
    }

    // 3. 민원 상세 페이지 이동
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable("id") Long id, Model model) {
        // 서비스에서 조회수 증가 로직과 DTO 변환을 한 번에 처리
        ComplaintDto complaintDto = complaintService.getComplaintDetail(id);

        model.addAttribute("complaint", complaintDto);

        return "inquiry/complaintDetail";
    }
}