package com.campuscare.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class GrievanceStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long grievanceId;

    private String status;

    private LocalDateTime updatedAt;

    public GrievanceStatusHistory() {
    }

    public GrievanceStatusHistory(
            Long grievanceId,
            String status,
            LocalDateTime updatedAt) {

        this.grievanceId = grievanceId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGrievanceId() {
        return grievanceId;
    }

    public void setGrievanceId(Long grievanceId) {
        this.grievanceId = grievanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}