package com.campuscare.service;

import com.campuscare.model.*;
import com.campuscare.repository.AttachmentRepository;
import com.campuscare.repository.GrievanceRepository;
import com.campuscare.repository.ResolverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GrievanceService {

    private final GrievanceRepository grievanceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final ResolverRepository resolverRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentRepository attachmentRepository;

    public GrievanceService(GrievanceRepository grievanceRepository,
                            EmailService emailService,
                            NotificationService notificationService,
                            ResolverRepository resolverRepository,
                            FileStorageService fileStorageService,
                            AttachmentRepository attachmentRepository) {
        this.grievanceRepository = grievanceRepository;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.resolverRepository = resolverRepository;
        this.fileStorageService = fileStorageService;
        this.attachmentRepository = attachmentRepository;
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
        return grievanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grievance not found with ID #" + id));
    }

    @Transactional
    public Grievance submitGrievance(Grievance grievance, String username) {
        grievance.setSubmittedBy(username);
        grievance.setStatus(GrievanceStatus.PENDING);
        grievance.setEscalated(false);
        grievance.setCreatedAt(LocalDateTime.now());
        grievance.setUpdatedAt(LocalDateTime.now());

        // Default Priority
        if (grievance.getPriority() == null) {
            grievance.setPriority(Priority.MEDIUM);
        }

        // Auto-calculate SLA Deadline
        switch (grievance.getPriority()) {
            case CRITICAL -> grievance.setSlaDeadline(LocalDateTime.now().plusHours(24));
            case HIGH -> grievance.setSlaDeadline(LocalDateTime.now().plusDays(2));
            case MEDIUM -> grievance.setSlaDeadline(LocalDateTime.now().plusDays(5));
            case LOW -> grievance.setSlaDeadline(LocalDateTime.now().plusDays(7));
        }

        // Department Routing
        if (grievance.getGrievanceType() != null) {
            if (grievance.getGrievanceType() == GrievanceType.HOSTEL) {
                grievance.setAssignedDepartment("HOSTEL");
                if (grievance.getDepartment() == null) {
                    grievance.setDepartment(Department.HOSTEL);
                }
            } else if (grievance.getGrievanceType() == GrievanceType.COLLEGE) {
                grievance.setAssignedDepartment("COLLEGE");
                if (grievance.getDepartment() == null) {
                    grievance.setDepartment(Department.ACADEMICS);
                }
            }
        }

        Grievance savedGrievance = grievanceRepository.save(grievance);

        // Notifications to Resolver
        Resolver resolver = resolverRepository.findFirstByDepartmentAndActiveTrue(savedGrievance.getAssignedDepartment());
        if (resolver != null) {
            emailService.sendResolverAlert(resolver.getEmail(), savedGrievance);
            notificationService.notifyAdmin(resolver.getPhoneNo(),
                    "URGENT: New Grievance #" + savedGrievance.getId() + " filed.");
        }

        // Notification to Student
        if (savedGrievance.getEmail() != null) {
            emailService.sendTicketCreationReceipt(savedGrievance.getEmail(), savedGrievance.getId(),
                    savedGrievance.getTitle(), savedGrievance.getAssignedDepartment());
        }

        return savedGrievance;
    }

    @Transactional
    public Grievance submitGrievanceWithAttachment(Grievance grievance, String username, MultipartFile file, UploadType uploadType) {
        Grievance saved = submitGrievance(grievance, username);
        if (file != null && !file.isEmpty()) {
            Attachment attachment = fileStorageService.saveFile(file, uploadType != null ? uploadType : UploadType.STUDENT_EVIDENCE, saved);
            if (attachment != null) {
                saved.addAttachment(attachment);
            }
        }
        return saved;
    }

    @Transactional
    public Grievance submitGrievanceWithFile(Grievance grievance, String username, MultipartFile file) {
        return submitGrievanceWithAttachment(grievance, username, file, UploadType.STUDENT_EVIDENCE);
    }

    @Transactional
    public Grievance markAsAwaitingVerification(Long id, String resolutionNotes) {
        Grievance grievance = getGrievanceById(id);
        grievance.setStatus(GrievanceStatus.AWAITING_VERIFICATION);
        grievance.setResolutionNotes(resolutionNotes);
        grievance.setUpdatedAt(LocalDateTime.now());
        return grievanceRepository.save(grievance);
    }

    @Transactional
    public Grievance markAsAwaitingVerificationWithAttachment(Long id, String resolutionNotes, MultipartFile file, UploadType uploadType) {
        Grievance grievance = markAsAwaitingVerification(id, resolutionNotes);
        if (file != null && !file.isEmpty()) {
            Attachment attachment = fileStorageService.saveFile(file, uploadType != null ? uploadType : UploadType.RESOLVER_PROOF, grievance);
            if (attachment != null) {
                grievance.addAttachment(attachment);
            }
        }
        return grievance;
    }

    @Transactional
    public Grievance markAsAwaitingVerificationWithFile(Long id, String resolutionNotes, MultipartFile file) {
        return markAsAwaitingVerificationWithAttachment(id, resolutionNotes, file, UploadType.RESOLVER_PROOF);
    }

    @Transactional
    public Grievance resolveGrievance(Long id) {
        return updateStatus(id, "RESOLVED", "Issue successfully resolved by administration.");
    }

    @Transactional
    public Grievance updateStatus(Long id, String newStatus, String actionRemarks) {
        return updateStatusWithAttachment(id, newStatus, actionRemarks, null, UploadType.RESOLVER_PROOF);
    }

    @Transactional
    public Grievance updateStatusWithAttachment(Long id, String newStatus, String actionRemarks, MultipartFile file, UploadType uploadType) {
        Grievance grievance = getGrievanceById(id);

        try {
            grievance.setStatus(GrievanceStatus.valueOf(newStatus.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
        }

        grievance.setActionTakenRemarks(actionRemarks);
        grievance.setUpdatedAt(LocalDateTime.now());

        if (grievance.getStatus() == GrievanceStatus.RESOLVED && grievance.getResolvedAt() == null) {
            grievance.setResolvedAt(LocalDateTime.now());
        }

        if (file != null && !file.isEmpty()) {
            Attachment attachment = fileStorageService.saveFile(file, uploadType != null ? uploadType : UploadType.RESOLVER_PROOF, grievance);
            if (attachment != null) {
                grievance.addAttachment(attachment);
            }
        }

        Grievance savedGrievance = grievanceRepository.save(grievance);

        if (savedGrievance.getStatus() == GrievanceStatus.RESOLVED) {
            emailService.sendResolutionNotification(savedGrievance.getEmail(), savedGrievance.getId(),
                    savedGrievance.getTitle(), actionRemarks);
        }

        return savedGrievance;
    }

    @Transactional
    public Grievance reopenGrievance(Long id, String feedback) {
        Grievance grievance = getGrievanceById(id);
        grievance.setStatus(GrievanceStatus.IN_PROGRESS);
        String currentNotes = grievance.getResolutionNotes() != null ? grievance.getResolutionNotes() : "";
        grievance.setResolutionNotes(currentNotes + "\n[REOPENED BY STUDENT]: " + feedback);
        grievance.setUpdatedAt(LocalDateTime.now());
        return grievanceRepository.save(grievance);
    }
}
