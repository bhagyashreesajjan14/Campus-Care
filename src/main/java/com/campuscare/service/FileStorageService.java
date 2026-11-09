package com.campuscare.service;

import com.campuscare.model.Attachment;
import com.campuscare.model.Grievance;
import com.campuscare.model.UploadType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    Attachment saveFile(MultipartFile file, String uploadType, Grievance grievance);

    Attachment saveFile(MultipartFile file, UploadType uploadType, Grievance grievance);

    Resource loadFileAsResource(String fileName);

    boolean deleteFile(String fileName);

    String storeFile(MultipartFile file);
}
