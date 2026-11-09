package com.campuscare.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "attachments")
@EntityListeners(AuditingEntityListener.class)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false, unique = true)
    private String storedFileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_type", nullable = false)
    private UploadType uploadType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grievance_id", nullable = false)
    @JsonIgnore
    private Grievance grievance;

    @CreatedDate
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    public Attachment() {
    }

    public Attachment(String originalFileName, String storedFileName, String fileType, Long fileSize,
                      UploadType uploadType, Grievance grievance) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadType = uploadType;
        this.grievance = grievance;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public UploadType getUploadType() {
        return uploadType;
    }

    public void setUploadType(UploadType uploadType) {
        this.uploadType = uploadType;
    }

    public Grievance getGrievance() {
        return grievance;
    }

    public void setGrievance(Grievance grievance) {
        this.grievance = grievance;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    // Backward-compatible accessors
    public String getFileName() {
        return originalFileName != null ? originalFileName : storedFileName;
    }

    public void setFileName(String fileName) {
        if (this.originalFileName == null) {
            this.originalFileName = fileName;
        }
        if (this.storedFileName == null) {
            this.storedFileName = fileName;
        }
    }

    public String getFilePath() {
        return storedFileName;
    }

    public void setFilePath(String filePath) {
        this.storedFileName = filePath;
    }

    // Utility methods for UI rendering
    public boolean isImage() {
        return fileType != null && fileType.toLowerCase().startsWith("image/");
    }

    public boolean isPdf() {
        return fileType != null && (fileType.equalsIgnoreCase("application/pdf")
                || (originalFileName != null && originalFileName.toLowerCase().endsWith(".pdf")));
    }

    public String getFormattedFileSize() {
        if (fileSize == null || fileSize <= 0) {
            return "0 B";
        }
        final String[] units = new String[] { "B", "KB", "MB", "GB" };
        int digitGroups = (int) (Math.log10(fileSize) / Math.log10(1024));
        return String.format("%.1f %s", fileSize / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
