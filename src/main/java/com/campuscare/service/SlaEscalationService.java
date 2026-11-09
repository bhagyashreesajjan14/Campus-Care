package com.campuscare.service;

import org.springframework.stereotype.Service;

@Service
public class SlaEscalationService {

    private final SlaEscalationScheduler scheduler;

    public SlaEscalationService(SlaEscalationScheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void triggerManualEscalation() {
        scheduler.runEscalationCycle();
    }
}
