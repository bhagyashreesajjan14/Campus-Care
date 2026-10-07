package com.campuscare.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendTicketCreationReceipt(String toEmail, Long ticketId, String title, String assignedDept) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Ticket Created: #" + ticketId + " - " + title);
            message.setText("Dear Student,\n\nYour grievance (Ticket ID: " + ticketId + ") has been successfully submitted and assigned to the " + assignedDept + " department.\n\nWe will notify you once there is an update.\n\nRegards,\nCampusCare Team");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email receipt: " + e.getMessage());
        }
    }

    @Async
    public void sendResolutionNotification(String toEmail, Long ticketId, String title, String remarks) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Ticket Resolved: #" + ticketId + " - " + title);
            message.setText("Dear Student,\n\nYour grievance (Ticket ID: " + ticketId + ") has been marked as RESOLVED.\n\nAction Taken Remarks: " + remarks + "\n\nPlease login to the dashboard to confirm the resolution or reopen the ticket if necessary.\n\nRegards,\nCampusCare Team");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send resolution email: " + e.getMessage());
        }
    }

    @Async
    public void sendResolverAlert(String toEmail, com.campuscare.model.Grievance grievance) {
        if (toEmail == null || toEmail.trim().isEmpty()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("URGENT: New Grievance Assigned - #" + grievance.getId());
            
            StringBuilder body = new StringBuilder();
            body.append("Dear Admin,\n\n");
            body.append("A new grievance has been routed to your department.\n\n");
            body.append("----- TICKET DETAILS -----\n");
            body.append("Ticket ID: #").append(grievance.getId()).append("\n");
            body.append("Title: ").append(grievance.getTitle()).append("\n");
            body.append("Description: ").append(grievance.getDescription()).append("\n");
            body.append("Priority: ").append(grievance.getPriority()).append("\n");
            
            if (grievance.getGrievanceType() == com.campuscare.model.GrievanceType.HOSTEL) {
                body.append("Hostel Block: ").append(grievance.getHostelBlock() != null ? grievance.getHostelBlock() : "N/A").append("\n");
                body.append("Room No: ").append(grievance.getRoomNo() != null ? grievance.getRoomNo() : "N/A").append("\n");
            } else if (grievance.getGrievanceType() == com.campuscare.model.GrievanceType.COLLEGE) {
                body.append("Department: ").append(grievance.getAcademicDepartment() != null ? grievance.getAcademicDepartment() : "N/A").append("\n");
                body.append("College Block: ").append(grievance.getCollegeBlock() != null ? grievance.getCollegeBlock() : "N/A").append("\n");
                body.append("Classroom/Lab: ").append(grievance.getClassroomOrLab() != null ? grievance.getClassroomOrLab() : "N/A").append("\n");
            }
            
            body.append("\n----- STUDENT DETAILS -----\n");
            if (grievance.getAnonymous()) {
                body.append("Student Name: [HIDDEN FOR ANONYMITY]\n");
                body.append("Phone No: [HIDDEN FOR ANONYMITY]\n");
            } else {
                body.append("Student Name: ").append(grievance.getStudentName() != null ? grievance.getStudentName() : "N/A").append("\n");
                body.append("Phone No: ").append(grievance.getPhoneNo() != null ? grievance.getPhoneNo() : "N/A").append("\n");
            }
            
            body.append("\nLog in to the CampusCare portal to take action.\n\nRegards,\nCampusCare System");
            
            message.setText(body.toString());
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send resolver alert email: " + e.getMessage());
        }
    }
}
