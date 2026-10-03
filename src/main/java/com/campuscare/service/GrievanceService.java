package com.campuscare.service;

import com.campuscare.model.Department;
import com.campuscare.model.Grievance;
import com.campuscare.model.GrievanceStatus;
import com.campuscare.model.Priority;
import com.campuscare.repository.GrievanceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final com.campuscare.repository.ResolverRepository resolverRepository;

    public GrievanceService(GrievanceRepository grievanceRepository, EmailService emailService, NotificationService notificationService, com.campuscare.repository.ResolverRepository resolverRepository) {
        this.grievanceRepository = grievanceRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.resolverRepository = resolverRepository;
    }

    public List<Grievance> getGrievancesByStudent(String username) {
        return grievanceRepository.findBySubmittedBy(username);
    }

    public List<Grievance> getGrievancesByDepartment(Department department) {
        return grievanceRepository.findByDepartment(department);
    }

    public List<Grievance> getAllGrievances() {
        return grievanceRepository.findAll();
    }

    public Grievance getGrievanceById(Long id) {
        return grievanceRepository.findById(id).orElseThrow(() -> new RuntimeException("Grievance not found"));
    }

    public Grievance submitGrievance(Grievance grievance, String username) {
        grievance.setSubmittedBy(username);
        grievance.setStatus(GrievanceStatus.PENDING);
        grievance.setCreatedAt(LocalDateTime.now());
        grievance.setUpdatedAt(LocalDateTime.now());
        
        // Auto-route based on Category if needed, but assuming form sets Department directly.
        // Auto-calculate SLA based on Priority
        if (grievance.getPriority() == null) grievance.setPriority(Priority.MEDIUM);

        switch (grievance.getPriority()) {
            case CRITICAL:
                grievance.setSlaDeadline(LocalDateTime.now().plusHours(24));
                break;
            case HIGH:
                grievance.setSlaDeadline(LocalDateTime.now().plusDays(2));
                break;
            case MEDIUM:
                grievance.setSlaDeadline(LocalDateTime.now().plusDays(5));
                break;
            case LOW:
            default:
                grievance.setSlaDeadline(LocalDateTime.now().plusDays(7));
                break;
        }

        // Department Routing
        if (grievance.getGrievanceType() != null) {
            if (grievance.getGrievanceType() == com.campuscare.model.GrievanceType.HOSTEL) {
                grievance.setAssignedDepartment("HOSTEL");
            } else if (grievance.getGrievanceType() == com.campuscare.model.GrievanceType.COLLEGE) {
                grievance.setAssignedDepartment("COLLEGE");
            }
        }

        Grievance savedGrievance = grievanceRepository.save(grievance);
        
        // Notifications to Resolver
        com.campuscare.model.Resolver resolver = resolverRepository.findFirstByDepartmentAndActiveTrue(grievance.getAssignedDepartment());
        if (resolver != null) {
            emailService.sendResolverAlert(resolver.getEmail(), savedGrievance);
            notificationService.notifyAdmin(resolver.getPhoneNo(), "URGENT: New Grievance #" + savedGrievance.getId() + " filed.");
        }
        
        // Notification to Student
        if (grievance.getEmail() != null) {
            emailService.sendTicketCreationReceipt(grievance.getEmail(), savedGrievance.getId(), grievance.getTitle(), grievance.getAssignedDepartment());
        }

        return savedGrievance;
    }

    public Grievance markAsAwaitingVerification(Long id, String resolutionNotes) {
        Grievance grievance = getGrievanceById(id);
        grievance.setStatus(GrievanceStatus.AWAITING_VERIFICATION);
        grievance.setResolutionNotes(resolutionNotes);
        return grievanceRepository.save(grievance);
    }

    public Grievance resolveGrievance(Long id) {
        return updateStatus(id, "RESOLVED", "Issue successfully resolved by administration.");
    }

    public Grievance updateStatus(Long id, String newStatus, String actionRemarks) {
        Grievance grievance = getGrievanceById(id);
        
        try {
            grievance.setStatus(GrievanceStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException e) {
            // keep existing status if invalid
        }
        
        grievance.setActionTakenRemarks(actionRemarks);
        grievance.setUpdatedAt(LocalDateTime.now());
        
        Grievance savedGrievance = grievanceRepository.save(grievance);
        
        if (savedGrievance.getStatus() == GrievanceStatus.RESOLVED) {
            emailService.sendResolutionNotification(savedGrievance.getEmail(), savedGrievance.getId(), savedGrievance.getTitle(), actionRemarks);
        }
        return savedGrievance;
    }

    public Grievance reopenGrievance(Long id, String feedback) {
        Grievance grievance = getGrievanceById(id);
        grievance.setStatus(GrievanceStatus.IN_PROGRESS);
        grievance.setResolutionNotes(grievance.getResolutionNotes() + " | Reopened: " + feedback);
        return grievanceRepository.save(grievance);
    }
}