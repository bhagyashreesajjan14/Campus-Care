package com.campuscare.repository;

import com.campuscare.model.Department;
import com.campuscare.model.Grievance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {
    List<Grievance> findBySubmittedBy(String submittedBy);
    List<Grievance> findByDepartment(Department department);
    
    List<Grievance> findBySubmittedByAndGrievanceType(String submittedBy, com.campuscare.model.GrievanceType grievanceType);
    long countBySubmittedByAndGrievanceTypeAndStatus(String submittedBy, com.campuscare.model.GrievanceType grievanceType, com.campuscare.model.GrievanceStatus status);
    long countBySubmittedByAndGrievanceTypeAndStatusIn(String submittedBy, com.campuscare.model.GrievanceType grievanceType, List<com.campuscare.model.GrievanceStatus> statuses);

    List<Grievance> findByAssignedDepartment(String assignedDepartment);
    long countByAssignedDepartment(String assignedDepartment);
    long countByAssignedDepartmentAndStatus(String assignedDepartment, com.campuscare.model.GrievanceStatus status);
}