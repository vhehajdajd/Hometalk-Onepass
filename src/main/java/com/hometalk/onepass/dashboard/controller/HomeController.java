package com.hometalk.onepass.dashboard.controller;

import com.hometalk.onepass.auth.config.CustomUserDetails;
import com.hometalk.onepass.auth.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {

            model.addAttribute("isLoggedIn", true);
            model.addAttribute("userRole", userDetails.getRole().name());

        } else {

            model.addAttribute("isLoggedIn", false);
            model.addAttribute("userRole", "GUEST");
        }

        return "home";
    }

    @GetMapping("/service/{module}")
    public String moveService(
            @PathVariable String module,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {

            return "redirect:/home";
        }

        User.UserRole role = user.getRole();

        if (role == User.UserRole.MEMBER) {
            return "redirect:/home";
        }

        boolean isAdmin = role == User.UserRole.ADMIN;

        return switch (module) {
            case "billing"   -> "redirect:" + (isAdmin ? "/billing/admin/unpaid"     : "/billing");
            case "parking"   -> "redirect:" + (isAdmin ? "/admin/vehicle/approval"   : "/parking/visit");
            case "facility"  -> "redirect:" + (isAdmin ? "/reservation/admin/status" : "/reservation/apply");
            case "community" -> "redirect:/community/square/all";
            case "civil"     -> "redirect:/inquiries/list";
            case "notice"    -> "redirect:/notice";
            case "schedule"  -> "redirect:/schedule";
            default          -> "redirect:/home";
        };
    }
}