package com.campuscare.service;

import com.campuscare.model.Priority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class Fast2SmsNotificationServiceImpl implements NotificationService {

    @Value("${sms.gateway.apikey:dummy-api-key}")
    private String apiKey;

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

    @Async
    @Override
    public void sendEscalationSms(String phone, Long ticketId, String title, Priority priority) {
        if (phone == null || phone.trim().isEmpty()) return;
        String alertMessage = "[URGENT ESCALATION] Ticket #" + ticketId + " (" + title 
                + ") breached SLA deadline and escalated to " + priority + " priority. Immediate action required.";
        sendSms(phone, alertMessage);
    }

    private void sendSms(String phone, String message) {
        try {
            System.out.println("[Fast2SMS API] Sending SMS to " + phone + ": " + message);
            // In live production, execute HTTP POST/GET to Fast2SMS Bulk SMS API gateway:
            // String url = "https://www.fast2sms.com/dev/bulkV2?authorization=" + apiKey + "&route=q&message=" + URLEncoder.encode(message, StandardCharsets.UTF_8) + "&language=english&flash=0&numbers=" + phone;
            // new RestTemplate().getForObject(url, String.class);
        } catch (Exception e) {
            System.err.println("Failed to send SMS to " + phone + ": " + e.getMessage());
        }
    }
}
