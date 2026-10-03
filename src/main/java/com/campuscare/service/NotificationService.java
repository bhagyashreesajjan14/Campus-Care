package com.campuscare.service;

public interface NotificationService {
    void notifyStudent(String phone, String message);
    void notifyAdmin(String departmentPhone, String message);
}
