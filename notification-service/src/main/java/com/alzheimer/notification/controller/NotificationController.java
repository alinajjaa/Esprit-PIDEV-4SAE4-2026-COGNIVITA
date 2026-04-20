package com.alzheimer.notification.controller;

import com.alzheimer.notification.entity.*;
import com.alzheimer.notification.repository.EscalationRuleRepository;
import com.alzheimer.notification.repository.NotificationRepository;
import com.alzheimer.notification.service.EscalationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final EscalationRuleRepository escalationRuleRepository;
    private final EscalationService escalationService;

    public NotificationController(NotificationRepository notificationRepository,
                                   EscalationRuleRepository escalationRuleRepository,
                                   EscalationService escalationService) {
        this.notificationRepository = notificationRepository;
        this.escalationRuleRepository = escalationRuleRepository;
        this.escalationService = escalationService;
    }

    /** GET all notifications for a user (most recent first) */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationRecord>> getUserNotifications(@PathVariable Long userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * GET unread IN_APP notification count for badge display.
     * FIX: was counting all SENT records across all channels (email, SMS, in-app),
     * inflating the badge. Now counts only IN_APP + SENT records.
     */
    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@PathVariable Long userId) {
        long count = notificationRepository.countByUserIdAndChannelAndStatus(
                userId, NotificationChannel.IN_APP, NotificationStatus.SENT);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    /** GET recent notifications (last 7 days) */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<List<NotificationRecord>> getRecentNotifications(@PathVariable Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        return ResponseEntity.ok(notificationRepository.findRecentByUserId(userId, since));
    }

    /**
     * PATCH mark a single IN_APP notification as read.
     * Sets status from SENT → READ so it no longer counts toward the badge.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<NotificationRecord> markAsRead(@PathVariable Long id) {
        return notificationRepository.findById(id)
                .map(record -> {
                    if (record.getChannel() == NotificationChannel.IN_APP
                            && record.getStatus() == NotificationStatus.SENT) {
                        record.setStatus(NotificationStatus.READ);
                        notificationRepository.save(record);
                    }
                    return ResponseEntity.ok(record);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH mark all IN_APP notifications for a user as read.
     */
    @PatchMapping("/user/{userId}/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(@PathVariable Long userId) {
        List<NotificationRecord> unread = notificationRepository
                .findByUserIdAndChannelAndStatus(userId, NotificationChannel.IN_APP, NotificationStatus.SENT);
        unread.forEach(r -> r.setStatus(NotificationStatus.READ));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(Map.of("markedRead", unread.size()));
    }

    /** GET escalation chain for a patient */
    @GetMapping("/escalations/patient/{patientUserId}")
    public ResponseEntity<List<EscalationRule>> getEscalations(@PathVariable Long patientUserId) {
        return ResponseEntity.ok(escalationRuleRepository.findByPatientUserIdOrderByTriggeredAtDesc(patientUserId));
    }

    /** POST resolve an active escalation (e.g. patient confirmed medication taken) */
    @PostMapping("/escalations/resolve")
    public ResponseEntity<Map<String, String>> resolveEscalation(
            @RequestParam Long patientUserId,
            @RequestParam String escalationType,
            @RequestParam Long referenceId) {
        escalationService.resolveEscalation(patientUserId,
                EscalationType.valueOf(escalationType), referenceId);
        return ResponseEntity.ok(Map.of("status", "resolved"));
    }

    /** DELETE (dismiss) a single notification */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** GET all active escalations (admin/clinical dashboard view) */
    @GetMapping("/escalations/active")
    public ResponseEntity<List<EscalationRule>> getActiveEscalations() {
        return ResponseEntity.ok(escalationRuleRepository.findByActiveTrue());
    }
}
