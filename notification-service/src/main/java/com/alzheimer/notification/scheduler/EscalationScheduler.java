package com.alzheimer.notification.scheduler;

import com.alzheimer.notification.entity.EscalationRule;
import com.alzheimer.notification.repository.EscalationRuleRepository;
import com.alzheimer.notification.service.EscalationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EscalationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EscalationScheduler.class);

    private final EscalationRuleRepository escalationRuleRepository;
    private final EscalationService escalationService;

    @Value("${notification.escalation.missed-dose-hours:48}")
    private long missedDoseHours;

    @Value("${notification.escalation.caregiver-escalation-hours:24}")
    private long caregiverEscalationHours;

    public EscalationScheduler(EscalationRuleRepository escalationRuleRepository,
                                EscalationService escalationService) {
        this.escalationRuleRepository = escalationRuleRepository;
        this.escalationService = escalationService;
    }

    /**
     * Runs every hour. Advances escalation rules to the next contact level.
     *
     *   Level 0 → 1 (caregiver):  triggeredAt older than missedDoseHours
     *   Level 1 → 2 (doctor):     lastEscalatedAt older than caregiverEscalationHours
     *
     * FIX: findOverdueEscalations now returns ONLY level-0 rules (was level < 2,
     * mixing level-0 and level-1 into the same result set). The redundant
     * currentLevel == 0 guard is removed — the query guarantees it.
     */
    @Scheduled(fixedDelayString = "PT1H")
    public void processEscalations() {
        log.debug("[EscalationScheduler] Running escalation check...");

        // Level 0 → 1: initial missed-dose / risk escalation to caregiver
        LocalDateTime caregiverThreshold = LocalDateTime.now().minusHours(missedDoseHours);
        List<EscalationRule> level0Rules = escalationRuleRepository.findOverdueEscalations(caregiverThreshold);
        for (EscalationRule rule : level0Rules) {
            log.info("[EscalationScheduler] Escalating patient {} → caregiver", rule.getPatientUserId());
            escalationService.escalateToCaregiver(rule);
        }

        // Level 1 → 2: caregiver didn't resolve → escalate to doctor
        LocalDateTime doctorThreshold = LocalDateTime.now().minusHours(caregiverEscalationHours);
        List<EscalationRule> level1Rules = escalationRuleRepository.findCaregiverEscalationsOverdue(doctorThreshold);
        for (EscalationRule rule : level1Rules) {
            log.info("[EscalationScheduler] Escalating patient {} → doctor", rule.getPatientUserId());
            escalationService.escalateToDoctor(rule);
        }

        int total = level0Rules.size() + level1Rules.size();
        if (total > 0) {
            log.info("[EscalationScheduler] Processed {} escalation(s)", total);
        }
    }
}
