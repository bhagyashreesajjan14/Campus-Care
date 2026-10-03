package com.campuscare.controller;

import com.campuscare.model.AppUser;
import com.campuscare.repository.UserRepository;
import com.campuscare.service.GrievanceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dept-admin")
public class DeptAdminController {

    private final GrievanceService grievanceService;
    private final UserRepository userRepository;

    public DeptAdminController(GrievanceService grievanceService, UserRepository userRepository) {
        this.grievanceService = grievanceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        AppUser adminUser = userRepository.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("grievances", grievanceService.getGrievancesByDepartment(adminUser.getDepartment()));
        return "dept-admin-dashboard";
    }

    @PostMapping("/verify/{id}")
    public String markAwaitingVerification(@PathVariable Long id, @RequestParam String resolutionNotes) {
        grievanceService.markAsAwaitingVerification(id, resolutionNotes);
        return "redirect:/dept-admin/dashboard?updated";
    }
}
