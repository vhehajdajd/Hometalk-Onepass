package com.hometalk.onepass.inquiry.controller;

import com.hometalk.onepass.inquiry.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller; // RestController가 아님!
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller // HTML 뷰를 반환하기 위해 사용
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintPageController {

    private final ComplaintService complaintService;

    @GetMapping("/list")
    public String listPage(Model model) {
        // 1. 서비스에서 데이터를 가져와서
        model.addAttribute("complaints", complaintService.findAll());

        // 2. templates/complaints/list.html 파일을 찾아서 띄워줌
        return "inquiry/complaintlist";
    }

    // 2. 글쓰기 페이지
    @GetMapping("/write")
    public String writePage() {
        // templates/inquiry/complaintWrite.html 파일을 찾아서 보여줌
        return "inquiry/complaintWrite";
    }
}