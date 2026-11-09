package com.campuscare.controller;

import com.campuscare.model.Grievance;
import com.campuscare.service.GrievanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private final GrievanceService grievanceService;

    public AnalyticsController(GrievanceService grievanceService) {
        this.grievanceService = grievanceService;
    }

    @GetMapping
    @SuppressWarnings("null")
    public Map<String, Object> getAnalytics() {
        List<Grievance> allGrievances = grievanceService.getAllGrievances();
        
        Map<String, Object> analytics = new HashMap<>();
        
        // 1. Ticket distribution by department
        Map<String, Long> byDepartment = allGrievances.stream()
                .filter(g -> g.getAssignedDepartment() != null)
                .collect(Collectors.groupingBy(Grievance::getAssignedDepartment, Collectors.counting()));
        analytics.put("distributionByDepartment", byDepartment);
        
        // 2. SLA breach rate per department
        Map<String, Object> slaCompliance = new HashMap<>();
        for (String dept : byDepartment.keySet()) {
            long totalDept = allGrievances.stream().filter(g -> dept.equals(g.getAssignedDepartment())).count();
            long breached = allGrievances.stream()
                    .filter(g -> dept.equals(g.getAssignedDepartment()) && g.isOverdue())
                    .count();
            Map<String, Long> stats = new HashMap<>();
            stats.put("total", totalDept);
            stats.put("breached", breached);
            stats.put("met", totalDept - breached);
            slaCompliance.put(dept, stats);
        }
        analytics.put("slaCompliance", slaCompliance);
        
        // 3. Status breakdown
        Map<String, Long> statusBreakdown = allGrievances.stream()
                .filter(g -> g.getStatus() != null)
                .collect(Collectors.groupingBy(g -> g.getStatus().name(), Collectors.counting()));
        analytics.put("statusBreakdown", statusBreakdown);
        
        // 4. Average MTTR in hours
        List<Grievance> resolvedTickets = allGrievances.stream()
                .filter(g -> g.getStatus().name().equals("RESOLVED") && g.getCreatedAt() != null && g.getResolvedAt() != null)
                .collect(Collectors.toList());
        
        double avgMttr = 0.0;
        if (!resolvedTickets.isEmpty()) {
            double totalHours = resolvedTickets.stream()
                    .mapToDouble(g -> java.time.Duration.between(g.getCreatedAt(), g.getResolvedAt()).toMinutes() / 60.0)
                    .sum();
            avgMttr = totalHours / resolvedTickets.size();
        }
        analytics.put("averageMttrHours", Math.round(avgMttr * 100.0) / 100.0);
        
        return analytics;
    }
}
