package com.campuscare.service;

import com.campuscare.model.AppUser;
import com.campuscare.model.Grievance;
import com.campuscare.model.GrievanceStatus;
import com.campuscare.model.Priority;
import com.campuscare.model.Resolver;
import com.campuscare.repository.GrievanceRepository;
import com.campuscare.repository.ResolverRepository;
import com.campuscare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class SlaEscalationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SlaEscalationScheduler.class);
    private static final DateTimeFormatter AUDIT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final GrievanceRepository grievanceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ResolverRepository resolverRepository;

    public SlaEscalationScheduler(GrievanceRepository grievanceRepository,
                                  EmailService emailService,
                                  NotificationService notificationService,
                                  UserRepository userRepository,
                                  ResolverRepository resolverRepository) {
        this.grievanceRepository = grievanceRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.resolverRepository = resolverRepository;
    }

    /**
     * Executes every 15 minutes to inspect un-escalated active tickets that have breached their SLA deadline.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void runEscalationCycle() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Executing automated SLA Escalation check at {}", now.format(AUDIT_DATE_FORMAT));

        List<GrievanceStatus> activeStatuses = Arrays.asList(GrievanceStatus.PENDING, GrievanceStatus.IN_PROGRESS);
        List<Grievance> breachedTickets = grievanceRepository.findBreachedTickets(activeStatuses, now);

        if (breachedTickets.isEmpty()) {
            logger.info("SLA check completed: No breached tickets found.");
            return;
        }

        logger.warn("Found {} ticket(s) breaching SLA deadlines. Initiating automated escalation...", breachedTickets.size());

        for (Grievance grievance : breachedTickets) {
            try {
                escalateGrievance(grievance, now);
            } catch (Exception ex) {
                logger.error("Failed to escalate grievance ID #{}: {}", grievance.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void escalateGrievance(Grievance grievance, LocalDateTime timestamp) {
        // 1. Mark as escalated and record timestamp
        grievance.setEscalated(true);
        grievance.setEscalatedAt(timestamp);

        // 2. Priority escalation
        Priority currentPriority = grievance.getPriority() != null ? grievance.getPriority() : Priority.MEDIUM;
        Priority newPriority = bumpPriority(currentPriority);
        grievance.setPriority(newPriority);

        // 3. Append internal system audit note
        String formattedTime = timestamp.format(AUDIT_DATE_FORMAT);
        String auditNote = "[SYSTEM ESCALATION] Ticket breached SLA deadline at " + formattedTime + ". Flagged for Super Admin review.";
        String existingNotes = grievance.getResolutionNotes();
        if (existingNotes == null || existingNotes.trim().isEmpty()) {
            grievance.setResolutionNotes(auditNote);
        } else {
            grievance.setResolutionNotes(existingNotes + "\n" + auditNote);
        }

        // 4. Save updated entity
        Grievance updatedGrievance = grievanceRepository.save(grievance);
        logger.info("Ticket #{} successfully escalated. Priority changed from {} to {}.",
                updatedGrievance.getId(), currentPriority, newPriority);

        // 5. Trigger alerts to Super Admin & Department Head
        sendEscalationAlerts(updatedGrievance);
    }

    private Priority bumpPriority(Priority currentPriority) {
        if (currentPriority == null) {
            return Priority.HIGH;
        }
        return switch (currentPriority) {
            case LOW -> Priority.MEDIUM;
            case MEDIUM -> Priority.HIGH;
            case HIGH, CRITICAL -> Priority.CRITICAL;
        };
    }

    private void sendEscalationAlerts(Grievance grievance) {
        // Send alert to Super Admin(s)
        List<AppUser> superAdmins = userRepository.findAll().stream()
                .filter(u -> "ROLE_SUPER_ADMIN".equalsIgnoreCase(u.getRole()))
                .toList();

        if (!superAdmins.isEmpty()) {
            for (AppUser sa : superAdmins) {
                if (sa.getEmail() != null && !sa.getEmail().isBlank()) {
                    emailService.sendEscalationAlert(sa.getEmail(), grievance);
                }
            }
        } else {
            // Fallback default admin email
            emailService.sendEscalationAlert("admin@campuscare.com", grievance);
        }

        // Find department resolver/head
        String assignedDept = grievance.getAssignedDepartment();
        Resolver resolver = resolverRepository.findFirstByDepartmentAndActiveTrue(assignedDept);
        if (resolver != null) {
            if (resolver.getEmail() != null && !resolver.getEmail().isBlank()) {
                emailService.sendEscalationAlert(resolver.getEmail(), grievance);
            }
            if (resolver.getPhoneNo() != null && !resolver.getPhoneNo().isBlank()) {
                notificationService.sendEscalationSms(resolver.getPhoneNo(), grievance.getId(),
                        grievance.getTitle(), grievance.getPriority());
            }
        } else {
            // Check AppUser table for Department Admin
            userRepository.findAll().stream()
                    .filter(u -> "ROLE_DEPT_ADMIN".equalsIgnoreCase(u.getRole()))
                    .filter(u -> (u.getDepartment() != null && assignedDept != null &&
                            assignedDept.equalsIgnoreCase(u.getDepartment().name())))
                    .findFirst()
                    .ifPresent(adminUser -> {
                        if (adminUser.getEmail() != null) {
                            emailService.sendEscalationAlert(adminUser.getEmail(), grievance);
                        }
                    });
        }
    }
}

