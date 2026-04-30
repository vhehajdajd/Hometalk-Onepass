package com.hometalk.onepass.complaint.controller;

import com.hometalk.onepass.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller; // RestController가 아님!
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/complaints")
@RequiredArgsConstructor
public class ComplaintPageController {

    private final ComplaintService complaintService;

    @GetMapping("/list")
    public String listPage(Model model) {
        model.addAttribute("complaints", complaintService.findAll());

        return "inquiry/complaintList";
    }

    @GetMapping("/write")
    public String writePage() {

        return "inquiry/complaintWrite";
    }
}