package com.campuscare.repository;

import com.campuscare.model.Attachment;
import com.campuscare.model.UploadType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByGrievanceId(Long grievanceId);
    List<Attachment> findByGrievanceIdAndUploadType(Long grievanceId, UploadType uploadType);
}
