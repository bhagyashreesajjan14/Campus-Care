package com.campuscare.service;

import com.campuscare.model.Priority;

public interface NotificationService {

    void notifyStudent(String phone, String message);

    void notifyAdmin(String departmentPhone, String message);

    void sendEscalationSms(String phone, Long ticketId, String title, Priority priority);
}
