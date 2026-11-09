package com.campuscare.controller;

import com.campuscare.model.AppUser;
import com.campuscare.model.Attachment;
import com.campuscare.model.Grievance;
import com.campuscare.repository.AttachmentRepository;
import com.campuscare.repository.UserRepository;
import com.campuscare.service.FileStorageService;
import com.campuscare.service.GrievanceService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class GrievanceController {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;
    private final GrievanceService grievanceService;

    public GrievanceController(AttachmentRepository attachmentRepository,
                               FileStorageService fileStorageService,
                               UserRepository userRepository,
                               GrievanceService grievanceService) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageService = fileStorageService;
        this.userRepository = userRepository;
        this.grievanceService = grievanceService;
    }

    /**
     * Securely serves the attachment (Image preview / PDF download) after verifying
     * that the requesting user has role-based permission to view the grievance.
     */
    @GetMapping("/attachments/{id}")
    @ResponseBody
    public ResponseEntity<Resource> getAttachment(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in to access this attachment.");
        }

        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found with ID #" + id));

        Grievance grievance = attachment.getGrievance();
        if (grievance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Associated grievance could not be found.");
        }

        // Role-based authorization check
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        boolean isDeptAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DEPT_ADMIN"));
        boolean isStudent = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));

        boolean authorized = false;

        if (isSuperAdmin) {
            authorized = true;
        } else if (isDeptAdmin) {
            AppUser admin = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (admin != null) {
                if (admin.getDepartment() != null && grievance.getDepartment() != null
                        && admin.getDepartment() == grievance.getDepartment()) {
                    authorized = true;
                } else if (admin.getDepartment() != null && grievance.getAssignedDepartment() != null
                        && admin.getDepartment().name().equalsIgnoreCase(grievance.getAssignedDepartment())) {
                    authorized = true;
                } else if (admin.getDepartment() == null) {
                    authorized = true;
                }
            }
        } else if (isStudent) {
            if (grievance.getSubmittedBy() != null && grievance.getSubmittedBy().equalsIgnoreCase(authentication.getName())) {
                authorized = true;
            }
        }

        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view attachments for this ticket.");
        }

        Resource resource = fileStorageService.loadFileAsResource(attachment.getStoredFileName());

        String contentType = attachment.getFileType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        // Inline disposition enables image rendering & PDF previews in the browser
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFileName() + "\"")
                .body(resource);
    }
}

