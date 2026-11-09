package com.campuscare.repository;

import com.campuscare.model.Department;
import com.campuscare.model.Grievance;
import com.campuscare.model.GrievanceStatus;
import com.campuscare.model.GrievanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GrievanceRepository extends JpaRepository<Grievance, Long> {

    List<Grievance> findBySubmittedBy(String submittedBy);

    List<Grievance> findByDepartment(Department department);

    List<Grievance> findBySubmittedByAndGrievanceType(String submittedBy, GrievanceType grievanceType);

    long countBySubmittedByAndGrievanceTypeAndStatus(String submittedBy, GrievanceType grievanceType, GrievanceStatus status);

    long countBySubmittedByAndGrievanceTypeAndStatusIn(String submittedBy, GrievanceType grievanceType, List<GrievanceStatus> statuses);

    List<Grievance> findByAssignedDepartment(String assignedDepartment);

    long countByAssignedDepartment(String assignedDepartment);

    long countByAssignedDepartmentAndStatus(String assignedDepartment, GrievanceStatus status);

    @Query("SELECT g FROM Grievance g WHERE g.status IN :statuses AND g.slaDeadline < :now AND g.isEscalated = false")
    List<Grievance> findBreachedTickets(
            @Param("statuses") List<GrievanceStatus> statuses,
            @Param("now") LocalDateTime now
    );

    List<Grievance> findByStatusInAndSlaDeadlineBeforeAndIsEscalatedFalse(
            List<GrievanceStatus> statuses,
            LocalDateTime deadline
    );

    default List<Grievance> findByStatusInAndSlaDeadlineBeforeAndEscalatedFalse(
            List<GrievanceStatus> statuses,
            LocalDateTime deadline) {
        return findByStatusInAndSlaDeadlineBeforeAndIsEscalatedFalse(statuses, deadline);
    }
}
