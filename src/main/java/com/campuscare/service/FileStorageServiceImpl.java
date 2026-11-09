package com.campuscare.service;

import com.campuscare.model.Attachment;
import com.campuscare.model.Grievance;
import com.campuscare.model.UploadType;
import com.campuscare.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;
    private final AttachmentRepository attachmentRepository;

    public FileStorageServiceImpl(
            @Value("${app.upload.dir:${upload.directory:uploads/}}") String uploadDir,
            AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the upload directory at " + this.fileStorageLocation, ex);
        }
    }

    @Override
    public Attachment saveFile(MultipartFile file, String uploadType, Grievance grievance) {
        UploadType parsedType;
        try {
            parsedType = UploadType.valueOf(uploadType.toUpperCase().trim());
        } catch (Exception e) {
            parsedType = UploadType.STUDENT_EVIDENCE;
        }
        return saveFile(file, parsedType, grievance);
    }

    @Override
    public Attachment saveFile(MultipartFile file, UploadType uploadType, Grievance grievance) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed"));

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file path sequence in filename: " + originalFileName);
        }

        // Sanitize base filename and prepend UUID
        String safeBaseName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = UUID.randomUUID().toString() + "_" + safeBaseName;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = new Attachment();
            attachment.setOriginalFileName(originalFileName);
            attachment.setStoredFileName(storedFileName);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setUploadType(uploadType);
            attachment.setUploadedAt(LocalDateTime.now());
            attachment.setGrievance(grievance);

            return attachmentRepository.save(attachment);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + storedFileName + " on disk.", ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                throw new SecurityException("Cannot access files outside the storage directory.");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found or unreadable: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Malformed URL for file: " + fileName, ex);
        }
    }

    @Override
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (!filePath.startsWith(this.fileStorageLocation)) {
                return false;
            }
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed"));
        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid path sequence: " + originalFileName);
        }
        String safeBaseName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = UUID.randomUUID().toString() + "_" + safeBaseName;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + storedFileName, ex);
        }
    }
}

