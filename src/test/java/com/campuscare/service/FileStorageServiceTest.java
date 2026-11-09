package com.campuscare.service;

import com.campuscare.model.Attachment;
import com.campuscare.model.Grievance;
import com.campuscare.model.UploadType;
import com.campuscare.repository.AttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FileStorageServiceTest {

    @TempDir
    Path tempUploadDir;

    private AttachmentRepository attachmentRepository;
    private FileStorageServiceImpl fileStorageService;

    @BeforeEach
    void setUp() {
        attachmentRepository = Mockito.mock(AttachmentRepository.class);
        when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        fileStorageService = new FileStorageServiceImpl(tempUploadDir.toString(), attachmentRepository);
    }

    @Test
    void testSaveFile_SuccessWithStudentEvidence() {
        Grievance grievance = new Grievance();
        grievance.setId(101L);

        MockMultipartFile file = new MockMultipartFile(
                "attachment",
                "broken_tap.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );

        Attachment saved = fileStorageService.saveFile(file, UploadType.STUDENT_EVIDENCE, grievance);

        assertNotNull(saved);
        assertEquals("broken_tap.jpg", saved.getOriginalFileName());
        assertTrue(saved.getStoredFileName().endsWith("_broken_tap.jpg"));
        assertEquals("image/jpeg", saved.getFileType());
        assertEquals(UploadType.STUDENT_EVIDENCE, saved.getUploadType());
        assertEquals(grievance, saved.getGrievance());
        assertTrue(saved.isImage());
    }

    @Test
    void testSaveFile_PathTraversalPrevention() {
        Grievance grievance = new Grievance();
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "attachment",
                "../../etc/passwd",
                "text/plain",
                "malicious".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.saveFile(maliciousFile, UploadType.STUDENT_EVIDENCE, grievance)
        );
    }

    @Test
    void testLoadFileAsResource_SuccessAndNotFound() {
        Grievance grievance = new Grievance();
        MockMultipartFile file = new MockMultipartFile(
                "attachment",
                "test.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        Attachment saved = fileStorageService.saveFile(file, UploadType.RESOLVER_PROOF, grievance);
        assertNotNull(saved);

        Resource resource = fileStorageService.loadFileAsResource(saved.getStoredFileName());
        assertTrue(resource.exists());
        assertTrue(resource.isReadable());

        assertThrows(RuntimeException.class, () ->
                fileStorageService.loadFileAsResource("non_existent_file.pdf")
        );
    }

    @Test
    void testDeleteFile() {
        Grievance grievance = new Grievance();
        MockMultipartFile file = new MockMultipartFile(
                "attachment",
                "to_delete.png",
                "image/png",
                "png bytes".getBytes()
        );

        Attachment saved = fileStorageService.saveFile(file, UploadType.STUDENT_EVIDENCE, grievance);
        assertTrue(fileStorageService.deleteFile(saved.getStoredFileName()));
        assertFalse(fileStorageService.deleteFile(saved.getStoredFileName())); // Already deleted
    }
}

