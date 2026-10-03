package com.campuscare.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class Fast2SmsNotificationServiceImpl implements NotificationService {

    @Value("${sms.gateway.apikey:dummy-api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    @Override
    public void notifyStudent(String phone, String message) {
        if (phone == null || phone.trim().isEmpty()) return;
        sendSms(phone, message);
    }

    @Async
    @Override
    public void notifyAdmin(String departmentPhone, String message) {
        if (departmentPhone == null || departmentPhone.trim().isEmpty()) return;
        sendSms(departmentPhone, message);
    }

    private void sendSms(String phone, String message) {
        try {
            System.out.println("[SMS API CALL MOCK] Sending SMS to " + phone + ": " + message);
            // Example production implementation:
            // String url = "https://www.fast2sms.com/dev/bulkV2?authorization=" + apiKey + "&route=q&message=" + message + "&language=english&flash=0&numbers=" + phone;
            // restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            System.err.println("Failed to send SMS to " + phone + ": " + e.getMessage());
        }
    }
}
