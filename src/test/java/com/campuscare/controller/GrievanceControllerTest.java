package com.campuscare.controller;

import com.campuscare.model.*;
import com.campuscare.repository.AttachmentRepository;
import com.campuscare.repository.UserRepository;
import com.campuscare.service.FileStorageService;
import com.campuscare.service.GrievanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GrievanceControllerTest {

    private AttachmentRepository attachmentRepository;
    private FileStorageService fileStorageService;
    private UserRepository userRepository;
    private GrievanceService grievanceService;
    private GrievanceController controller;

    @BeforeEach
    void setUp() {
        attachmentRepository = Mockito.mock(AttachmentRepository.class);
        fileStorageService = Mockito.mock(FileStorageService.class);
        userRepository = Mockito.mock(UserRepository.class);
        grievanceService = Mockito.mock(GrievanceService.class);

        controller = new GrievanceController(
                attachmentRepository,
                fileStorageService,
                userRepository,
                grievanceService
        );
    }

    @Test
    void testGetAttachment_SuperAdmin_Permitted() {
        Grievance grievance = new Grievance();
        grievance.setId(10L);
        grievance.setSubmittedBy("other_student");

        Attachment attachment = new Attachment();
        attachment.setId(1L);
        attachment.setOriginalFileName("evidence.jpg");
        attachment.setStoredFileName("uuid_evidence.jpg");
        attachment.setFileType("image/jpeg");
        attachment.setGrievance(grievance);

        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));
        Resource mockResource = new ByteArrayResource("test".getBytes());
        when(fileStorageService.loadFileAsResource("uuid_evidence.jpg")).thenReturn(mockResource);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "superadmin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );

        ResponseEntity<Resource> response = controller.getAttachment(1L, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetAttachment_StudentOwner_Permitted() {
        Grievance grievance = new Grievance();
        grievance.setId(10L);
        grievance.setSubmittedBy("alice");

        Attachment attachment = new Attachment();
        attachment.setId(2L);
        attachment.setOriginalFileName("doc.pdf");
        attachment.setStoredFileName("uuid_doc.pdf");
        attachment.setFileType("application/pdf");
        attachment.setGrievance(grievance);

        when(attachmentRepository.findById(2L)).thenReturn(Optional.of(attachment));
        Resource mockResource = new ByteArrayResource("test".getBytes());
        when(fileStorageService.loadFileAsResource("uuid_doc.pdf")).thenReturn(mockResource);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", "pwd", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        ResponseEntity<Resource> response = controller.getAttachment(2L, auth);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testGetAttachment_DifferentStudent_Forbidden() {
        Grievance grievance = new Grievance();
        grievance.setId(10L);
        grievance.setSubmittedBy("alice");

        Attachment attachment = new Attachment();
        attachment.setId(3L);
        attachment.setStoredFileName("uuid_file.png");
        attachment.setGrievance(grievance);

        when(attachmentRepository.findById(3L)).thenReturn(Optional.of(attachment));

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "bob", "pwd", List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                controller.getAttachment(3L, auth)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void testGetAttachment_Unauthenticated_Unauthorized() {
        assertThrows(ResponseStatusException.class, () ->
                controller.getAttachment(1L, null)
        );
    }
}

