package com.campuscare.service;

import com.campuscare.model.*;
import com.campuscare.repository.GrievanceRepository;
import com.campuscare.repository.ResolverRepository;
import com.campuscare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SlaEscalationSchedulerTest {

    private GrievanceRepository grievanceRepository;
    private EmailService emailService;
    private NotificationService notificationService;
    private UserRepository userRepository;
    private ResolverRepository resolverRepository;
    private SlaEscalationScheduler scheduler;

    @BeforeEach
    void setUp() {
        grievanceRepository = Mockito.mock(GrievanceRepository.class);
        emailService = Mockito.mock(EmailService.class);
        notificationService = Mockito.mock(NotificationService.class);
        userRepository = Mockito.mock(UserRepository.class);
        resolverRepository = Mockito.mock(ResolverRepository.class);

        scheduler = new SlaEscalationScheduler(
                grievanceRepository,
                emailService,
                notificationService,
                userRepository,
                resolverRepository
        );
    }

    @Test
    void testRunEscalationCycle_BreachedTicketGetsEscalated() {
        Grievance breachedGrievance = new Grievance();
        breachedGrievance.setId(42L);
        breachedGrievance.setTitle("Hostel geyser not working");
        breachedGrievance.setStatus(GrievanceStatus.PENDING);
        breachedGrievance.setPriority(Priority.MEDIUM);
        breachedGrievance.setSlaDeadline(LocalDateTime.now().minusHours(2));
        breachedGrievance.setEscalated(false);
        breachedGrievance.setAssignedDepartment("HOSTEL");

        when(grievanceRepository.findBreachedTickets(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of(breachedGrievance));
        when(grievanceRepository.save(any(Grievance.class))).thenAnswer(i -> i.getArgument(0));

        AppUser superAdmin = new AppUser();
        superAdmin.setEmail("admin@campuscare.com");
        superAdmin.setRole("ROLE_SUPER_ADMIN");
        when(userRepository.findAll()).thenReturn(List.of(superAdmin));

        Resolver resolver = new Resolver();
        resolver.setEmail("hostelhead@campuscare.com");
        resolver.setPhoneNo("9876543210");
        when(resolverRepository.findFirstByDepartmentAndActiveTrue("HOSTEL")).thenReturn(resolver);

        scheduler.runEscalationCycle();

        // 1. Verify grievance was saved with escalated fields
        ArgumentCaptor<Grievance> captor = ArgumentCaptor.forClass(Grievance.class);
        verify(grievanceRepository).save(captor.capture());
        Grievance saved = captor.getValue();

        assertTrue(saved.isEscalated());
        assertNotNull(saved.getEscalatedAt());
        assertEquals(Priority.HIGH, saved.getPriority()); // MEDIUM bumped to HIGH
        assertNotNull(saved.getResolutionNotes());
        assertTrue(saved.getResolutionNotes().contains("[SYSTEM ESCALATION] Ticket breached SLA deadline"));

        // 2. Verify notifications
        verify(emailService, atLeastOnce()).sendEscalationAlert(eq("admin@campuscare.com"), any(Grievance.class));
        verify(emailService, atLeastOnce()).sendEscalationAlert(eq("hostelhead@campuscare.com"), any(Grievance.class));
        verify(notificationService).sendEscalationSms(eq("9876543210"), eq(42L), anyString(), eq(Priority.HIGH));
    }

    @Test
    void testRunEscalationCycle_NoBreachedTickets() {
        when(grievanceRepository.findBreachedTickets(anyList(), any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.runEscalationCycle();

        verify(grievanceRepository, never()).save(any());
        verifyNoInteractions(emailService);
        verifyNoInteractions(notificationService);
    }
}

