package com.campuscare.controller;

import com.campuscare.service.GrievanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    private final GrievanceService grievanceService;

    public SuperAdminController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("grievances", grievanceService.getAllGrievances());
        return "super-admin-dashboard";
    }
}
