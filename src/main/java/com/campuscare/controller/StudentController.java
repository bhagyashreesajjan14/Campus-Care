package com.campuscare.controller;

import com.campuscare.model.AppUser;
import com.campuscare.model.Grievance;
import com.campuscare.model.GrievanceStatus;
import com.campuscare.repository.UserRepository;
import com.campuscare.service.GrievanceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final GrievanceService grievanceService;
    private final UserRepository userRepository;

    public StudentController(GrievanceService grievanceService, UserRepository userRepository) {
        this.grievanceService = grievanceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {
        AppUser student = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Grievance> myTickets = grievanceService.getGrievancesByStudent(auth.getName());
        myTickets.sort(java.util.Comparator.comparing(Grievance::getCreatedAt).reversed());
        
        long pendingCount = myTickets.stream().filter(g -> g.getStatus() == GrievanceStatus.PENDING).count();
        long inProgressCount = myTickets.stream().filter(g -> 
            g.getStatus() == GrievanceStatus.IN_PROGRESS || 
            g.getStatus() == GrievanceStatus.ASSIGNED || 
            g.getStatus() == GrievanceStatus.ESCALATED || 
            g.getStatus() == GrievanceStatus.AWAITING_VERIFICATION).count();
        long resolvedCount = myTickets.stream().filter(g -> g.getStatus() == GrievanceStatus.RESOLVED).count();
        
        model.addAttribute("studentName", student.getName());
        model.addAttribute("totalCount", myTickets.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("myTickets", myTickets);
        
        return "student-dashboard";
    }

    @GetMapping("/file-grievance")
    public String fileGrievanceForm(@RequestParam(required=false) String type, Model model) {
        Grievance grievance = new Grievance();
        if (type != null) {
            try {
                grievance.setGrievanceType(com.campuscare.model.GrievanceType.valueOf(type.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        model.addAttribute("grievance", grievance);
        return "student-file-grievance";
    }

    @PostMapping("/file-grievance")
    public String submitGrievance(@ModelAttribute Grievance grievance, Authentication auth, RedirectAttributes redirectAttributes) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                throw new IllegalStateException("User session not found or unauthenticated.");
            }
            AppUser student = userRepository.findByUsername(auth.getName()).orElseThrow();
            grievance.setStudentName(student.getName());
            grievanceService.submitGrievance(grievance, auth.getName());
            return "redirect:/student/dashboard?success";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit grievance: " + e.getMessage());
            return "redirect:/student/file-grievance?error";
        }
    }

    @PostMapping("/resolve/{id}")
    public String resolveTicket(@PathVariable Long id) {
        grievanceService.resolveGrievance(id);
        return "redirect:/student/dashboard?resolved";
    }

    @PostMapping("/reopen/{id}")
    public String reopenTicket(@PathVariable Long id, @RequestParam String feedback) {
        grievanceService.reopenGrievance(id, feedback);
        return "redirect:/student/dashboard?reopened";
    }
}
