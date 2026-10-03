package com.campuscare.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Grievance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String studentName;
    private String usn;
    private String email;
    private String phoneNo;
    private String roomNo;
    private String submittedBy; // Username

    @Column(name = "is_anonymous")
    private Boolean anonymous = false;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    private GrievanceType grievanceType;

    private String category;
    
    private String assignedDepartment;

    @Enumerated(EnumType.STRING)
    private Department department;

    private String hostelBlock;
    private String academicDepartment;
    private String collegeBlock;
    private String classroomOrLab;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    private GrievanceStatus status;

    private LocalDateTime slaDeadline;

    @Column(length = 2000)
    private String resolutionNotes;
    
    @Column(length = 2000)
    private String actionTakenRemarks;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Grievance() {
    }

    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getUsn() { return usn; }
    public void setUsn(String usn) { this.usn = usn; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNo() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }
    public String getRoomNo() { return roomNo; }
    public void setRoomNo(String roomNo) { this.roomNo = roomNo; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public Boolean isAnonymous() { return anonymous != null ? anonymous : false; }
    public void setAnonymous(Boolean anonymous) { this.anonymous = anonymous; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public GrievanceType getGrievanceType() { return grievanceType; }
    public void setGrievanceType(GrievanceType grievanceType) { this.grievanceType = grievanceType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAssignedDepartment() { return assignedDepartment; }
    public void setAssignedDepartment(String assignedDepartment) { this.assignedDepartment = assignedDepartment; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getHostelBlock() { return hostelBlock; }
    public void setHostelBlock(String hostelBlock) { this.hostelBlock = hostelBlock; }
    public String getAcademicDepartment() { return academicDepartment; }
    public void setAcademicDepartment(String academicDepartment) { this.academicDepartment = academicDepartment; }
    public String getCollegeBlock() { return collegeBlock; }
    public void setCollegeBlock(String collegeBlock) { this.collegeBlock = collegeBlock; }
    public String getClassroomOrLab() { return classroomOrLab; }
    public void setClassroomOrLab(String classroomOrLab) { this.classroomOrLab = classroomOrLab; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public GrievanceStatus getStatus() { return status; }
    public void setStatus(GrievanceStatus status) { this.status = status; }
    public LocalDateTime getSlaDeadline() { return slaDeadline; }
    public void setSlaDeadline(LocalDateTime slaDeadline) { this.slaDeadline = slaDeadline; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public String getActionTakenRemarks() { return actionTakenRemarks; }
    public void setActionTakenRemarks(String actionTakenRemarks) { this.actionTakenRemarks = actionTakenRemarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isOverdue() {
        if (slaDeadline == null || status == GrievanceStatus.RESOLVED || status == GrievanceStatus.AWAITING_VERIFICATION) {
            return false;
        }
        return LocalDateTime.now().isAfter(slaDeadline);
    }
}