package com.campuscare.controller;

import com.campuscare.model.Grievance;
import com.campuscare.model.GrievanceStatus;
import com.campuscare.repository.GrievanceRepository;
import com.campuscare.service.GrievanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final GrievanceRepository grievanceRepository;
    private final GrievanceService grievanceService;

    public AdminController(GrievanceRepository grievanceRepository, GrievanceService grievanceService) {
        this.grievanceRepository = grievanceRepository;
        this.grievanceService = grievanceService;
    }

    @GetMapping("/hostel/dashboard")
    public String hostelDashboard(Model model) {
        String dept = "HOSTEL";
        List<Grievance> tickets = grievanceRepository.findByAssignedDepartment(dept);
        
        long total = tickets.size();
        long inProgress = grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.IN_PROGRESS) +
                          grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.ASSIGNED) +
                          grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.ESCALATED);
        long resolved = grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.RESOLVED);
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("totalTickets", total);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("resolvedCount", resolved);
        
        return "admin-hostel-dashboard";
    }

    @GetMapping("/college/dashboard")
    public String collegeDashboard(Model model) {
        String dept = "COLLEGE";
        List<Grievance> tickets = grievanceRepository.findByAssignedDepartment(dept);
        
        long total = tickets.size();
        long inProgress = grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.IN_PROGRESS) +
                          grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.ASSIGNED) +
                          grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.ESCALATED);
        long resolved = grievanceRepository.countByAssignedDepartmentAndStatus(dept, GrievanceStatus.RESOLVED);
        
        model.addAttribute("tickets", tickets);
        model.addAttribute("totalTickets", total);
        model.addAttribute("inProgressCount", inProgress);
        model.addAttribute("resolvedCount", resolved);
        
        return "admin-college-dashboard";
    }

    @PostMapping("/grievance/update")
    public String updateGrievanceStatus(@RequestParam Long id, 
                                        @RequestParam String status, 
                                        @RequestParam String actionTakenRemarks,
                                        @RequestParam String sourceDept) {
        grievanceService.updateStatus(id, status, actionTakenRemarks);
        if ("HOSTEL".equalsIgnoreCase(sourceDept)) {
            return "redirect:/admin/hostel/dashboard?success";
        }
        return "redirect:/admin/college/dashboard?success";
    }
}
