package com.campuscare.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboardRedirect(Authentication authentication) {
        String username = authentication.getName();
        if ("hosteladmin".equals(username)) {
            return "redirect:/admin/hostel/dashboard";
        } else if ("collegeadmin".equals(username)) {
            return "redirect:/admin/college/dashboard";
        }
        
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (auth.getAuthority().equals("ROLE_STUDENT")) {
                return "redirect:/student/dashboard";
            } else if (auth.getAuthority().equals("ROLE_DEPT_ADMIN")) {
                return "redirect:/dept-admin/dashboard";
            } else if (auth.getAuthority().equals("ROLE_SUPER_ADMIN")) {
                return "redirect:/super-admin/dashboard";
            }
        }
        return "redirect:/login";
    }
}