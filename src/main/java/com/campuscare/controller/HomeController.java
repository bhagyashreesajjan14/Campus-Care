package com.campuscare.controller;

import com.campuscare.model.Grievance;
import com.campuscare.model.GrievanceStatus;
import com.campuscare.repository.UserRepository;
import com.campuscare.service.GrievanceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final GrievanceService grievanceService;
    private final UserRepository userRepository;

    public HomeController(GrievanceService grievanceService, UserRepository userRepository) {
        this.grievanceService = grievanceService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String index(Model model, Authentication auth) {
        // Safe defaults for unauthenticated users
        model.addAttribute("grievances", java.util.Collections.emptyList());
        model.addAttribute("totalSubmitted", 0L);
        model.addAttribute("inProgressCount", 0L);
        model.addAttribute("awaitingVerificationCount", 0L);
        model.addAttribute("resolvedCount", 0L);
        model.addAttribute("studentName", "Student");

        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            
            boolean isStudent = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
            
            if (isStudent) {
                userRepository.findByUsername(auth.getName()).ifPresent(student -> {
                    model.addAttribute("studentName", student.getName());
                });

                List<Grievance> grievances = grievanceService.getGrievancesByStudent(auth.getName());
                
                long inProgressCount = grievances.stream()
                    .filter(g -> g.getStatus() == GrievanceStatus.IN_PROGRESS || g.getStatus() == GrievanceStatus.ASSIGNED || g.getStatus() == GrievanceStatus.ESCALATED)
                    .count();
                    
                long awaitingCount = grievances.stream()
                    .filter(g -> g.getStatus() == GrievanceStatus.AWAITING_VERIFICATION)
                    .count();
                    
                long resolvedCount = grievances.stream()
                    .filter(g -> g.getStatus() == GrievanceStatus.RESOLVED)
                    .count();
                    
                model.addAttribute("grievances", grievances);
                model.addAttribute("totalSubmitted", grievances.size());
                model.addAttribute("inProgressCount", inProgressCount);
                model.addAttribute("awaitingVerificationCount", awaitingCount);
                model.addAttribute("resolvedCount", resolvedCount);
            }
        }
        return "index";
    }

    @GetMapping("/track")
    public String trackTicket(@org.springframework.web.bind.annotation.RequestParam String ticketId, Model model) {
        try {
            // Strip any non-numeric characters (like "CC-")
            String numericPart = ticketId.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                throw new IllegalArgumentException("Invalid Ticket ID format.");
            }
            Long id = Long.parseLong(numericPart);
            Grievance ticket = grievanceService.getGrievanceById(id);
            model.addAttribute("ticket", ticket);
        } catch (Exception e) {
            model.addAttribute("error", "Ticket not found or invalid format. Please use 'CC-1024' or just '1024'.");
        }
        return "track-ticket";
    }
}
