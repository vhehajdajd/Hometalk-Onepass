package com.hometalk.onepass.parking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffViewController {

    @GetMapping("/entry")
    public String entryPage() {
        return "parking/staff-entry";
    }

    @GetMapping("/manual-entry")
    public String manualEntryPage() {
        return "parking/staff-manual-entry";
    }

    @GetMapping("/exit")
    public String exitPage() {
        return "parking/staff-exit";
    }
}