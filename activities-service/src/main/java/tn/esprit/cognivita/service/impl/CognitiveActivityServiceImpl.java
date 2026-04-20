package tn.esprit.cognivita.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.cognivita.entity.CognitiveActivity;
import tn.esprit.cognivita.entity.ActivityParticipation;
import tn.esprit.cognivita.entity.ParticipationStatus;
import tn.esprit.cognivita.repository.CognitiveActivityRepository;
import tn.esprit.cognivita.repository.ActivityParticipationRepository;
import tn.esprit.cognivita.service.CognitiveActivityService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CognitiveActivityServiceImpl implements CognitiveActivityService {

    private final CognitiveActivityRepository activityRepository;
    private final ActivityParticipationRepository participationRepository;

    // ============= CRUD OPERATIONS =============

    @Override
    public CognitiveActivity createActivity(CognitiveActivity activity) {
        // Log pour déboguer
        System.out.println("📝 Création d'activité - Type: " + activity.getType());
        if (activity.isMemoryType()) {
            System.out.println("📚 Mots reçus: " + activity.getWords());
        } else if (activity.isAttentionType()) {
            System.out.println("👀 Stroop: " + activity.getStroopWord() +
                    " / " + activity.getStroopColor() +
                    " / " + activity.getStroopCorrect());
        } else if (activity.isLogicType()) {
            System.out.println("🔢 Séquence: " + activity.getSequence() +
                    " -> " + activity.getSequenceAnswer());
        }

        activity.setIsActive(true);
        // Utiliser saveAndFlush pour forcer le flush immédiat
        CognitiveActivity saved = activityRepository.saveAndFlush(activity);
        System.out.println("✅ Activité sauvegardée avec ID: " + saved.getId());

        // Debug SQL: afficher la taille de la collection après le flush
        System.out.println("🔍 Nombre de mots persistés (d'après l'entité retournée): " +
                (saved.getWords() == null ? 0 : saved.getWords().size()));
        return saved;
    }

    @Override
    public CognitiveActivity updateActivity(Long id, CognitiveActivity activityDetails) {
        CognitiveActivity activity = getActivityById(id);

        activity.setTitle(activityDetails.getTitle());
        activity.setDescription(activityDetails.getDescription());
        activity.setType(activityDetails.getType());
        activity.setDifficulty(activityDetails.getDifficulty());

        activity.setTimeLimit(activityDetails.getTimeLimit());
        activity.setMaxScore(activityDetails.getMaxScore());
        activity.setInstructions(activityDetails.getInstructions());
        activity.setImageUrl(activityDetails.getImageUrl());

        // ✅ Mettre à jour les nouveaux champs
        if (activity.isMemoryType()) {
            activity.setWords(activityDetails.getWords());
        } else if (activity.isAttentionType()) {
            activity.setStroopWord(activityDetails.getStroopWord());
            activity.setStroopColor(activityDetails.getStroopColor());
            activity.setStroopCorrect(activityDetails.getStroopCorrect());
        } else if (activity.isLogicType()) {
            activity.setSequence(activityDetails.getSequence());
            activity.setSequenceAnswer(activityDetails.getSequenceAnswer());
        }

        return activityRepository.save(activity);
    }

    @Override
    public void deleteActivity(Long id) {
        CognitiveActivity activity = getActivityById(id);
        activityRepository.delete(activity);
    }

    // ============= MÉTHODE CORRIGÉE AVEC LOGS =============
    @Override
    public CognitiveActivity getActivityById(Long id) {
        System.out.println("🔍 RECHERCHE DE L'ACTIVITÉ ID: " + id);

        CognitiveActivity activity = activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));

        // ===== LOGS DÉTAILLÉS =====
        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ ACTIVITÉ TROUVÉE - ID: " + id);
        System.out.println("📋 Titre: " + activity.getTitle());
        System.out.println("📝 Type: " + activity.getType());
        System.out.println("📊 Difficulté: " + activity.getDifficulty());
        System.out.println("⚡ Active: " + activity.getIsActive());

        // Logs spécifiques selon le type
        if (activity.isMemoryType()) {
            System.out.println("🧠 TYPE: MÉMOIRE");
            System.out.println("📚 Mots bruts: " + activity.getWords());
            System.out.println("📚 Nombre de mots: " + (activity.getWords() != null ? activity.getWords().size() : 0));

            if (activity.getWords() != null && !activity.getWords().isEmpty()) {
                System.out.println("📋 Liste des mots:");
                for (int i = 0; i < activity.getWords().size(); i++) {
                    System.out.println("   " + (i+1) + ". '" + activity.getWords().get(i) + "'");
                }
            } else {
                System.out.println("⚠️ AUCUN MOT TROUVÉ DANS L'ACTIVITÉ!");
            }
        }
        else if (activity.isAttentionType()) {
            System.out.println("👀 TYPE: ATTENTION");
            System.out.println("   Mot affiché: " + activity.getStroopWord());
            System.out.println("   Couleur du texte: " + activity.getStroopColor());
            System.out.println("   Réponse correcte: " + activity.getStroopCorrect());
        }
        else if (activity.isLogicType()) {
            System.out.println("🔢 TYPE: LOGIQUE");
            System.out.println("   Séquence: " + activity.getSequence());
            System.out.println("   Réponse attendue: " + activity.getSequenceAnswer());
        }

        System.out.println("═══════════════════════════════════════\n");

        return activity;
    }

    @Override
    public List<CognitiveActivity> getAllActivities() {
        return activityRepository.findByIsActiveTrue();
    }

    // ============= FILTERS =============

    @Override
    public List<CognitiveActivity> getActivitiesByType(String type) {
        return activityRepository.findByType(type);
    }

    @Override
    public List<CognitiveActivity> getActivitiesByDifficulty(String difficulty) {
        return activityRepository.findByDifficulty(difficulty);
    }

    @Override
    public List<CognitiveActivity> filterActivities(String type, String difficulty) {
        return activityRepository.filterActivities(type, difficulty);
    }

    @Override
    public List<CognitiveActivity> searchActivities(String keyword) {
        return activityRepository.searchByTitleOrDescription(keyword);
    }

    // ============= ACTIVITY STATUS =============

    @Override
    public void deactivateActivity(Long id) {
        CognitiveActivity activity = getActivityById(id);
        activity.setIsActive(false);
        activityRepository.save(activity);
    }

    // ============= PARTICIPATION =============

    @Override
    public ActivityParticipation startActivity(Long activityId, Long userId) {
        CognitiveActivity activity = getActivityById(activityId);

        ActivityParticipation participation = new ActivityParticipation();
        participation.setActivity(activity);
        participation.setUserId(userId);
        participation.setStatus(ParticipationStatus.IN_PROGRESS);
        participation.setStartTime(LocalDateTime.now());

        return participationRepository.save(participation);
    }

    @Override
    public ActivityParticipation completeActivity(Long participationId, Integer score, Integer timeSpent) {
        ActivityParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new RuntimeException("Participation not found with id: " + participationId));

        participation.setScore(score);
        participation.setTimeSpent(timeSpent);
        participation.setCompleted(true);
        participation.setStatus(ParticipationStatus.COMPLETED);
        participation.setCompletedAt(LocalDateTime.now());
        participation.setEndTime(LocalDateTime.now());

        return participationRepository.save(participation);
    }

    @Override
    public ActivityParticipation abandonActivity(Long participationId) {
        ActivityParticipation participation = participationRepository.findById(participationId)
                .orElseThrow(() -> new RuntimeException("Participation not found with id: " + participationId));

        participation.setAbandoned(true);
        participation.setStatus(ParticipationStatus.ABANDONED);
        participation.setEndTime(LocalDateTime.now());

        return participationRepository.save(participation);
    }

    // ============= USER HISTORY =============

    @Override
    public List<ActivityParticipation> getUserHistory(Long userId) {
        return participationRepository.findByUserId(userId);
    }

    @Override
    public List<CognitiveActivity> getUserCompletedActivities(Long userId) {
        List<ActivityParticipation> participations =
                participationRepository.findByUserIdAndStatus(userId, ParticipationStatus.COMPLETED);

        return participations.stream()
                .map(ActivityParticipation::getActivity)
                .distinct()
                .collect(Collectors.toList());
    }

    // ============= RECOMMENDATIONS =============

    @Override
    public List<CognitiveActivity> getRecommendationsForUser(Long userId) {
        List<CognitiveActivity> completedActivities = getUserCompletedActivities(userId);
        Set<Long> completedIds = completedActivities.stream()
                .map(CognitiveActivity::getId)
                .collect(Collectors.toSet());

        List<CognitiveActivity> allActivities = activityRepository.findByIsActiveTrue();

        return allActivities.stream()
                .filter(activity -> !completedIds.contains(activity.getId()))
                .limit(5)
                .collect(Collectors.toList());
    }

    // ============= STATISTICS =============

    @Override
    public Map<String, Object> getUserStatistics(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("userId", userId);
        stats.put("totalActivities", participationRepository.countByUserId(userId));
        stats.put("completedActivities",
                participationRepository.countByUserIdAndStatus(userId, ParticipationStatus.COMPLETED));
        stats.put("abandonedActivities",
                participationRepository.countByUserIdAndStatus(userId, ParticipationStatus.ABANDONED));
        stats.put("averageScore", participationRepository.calculateAverageScore(userId));
        stats.put("totalPoints", participationRepository.calculateTotalPoints(userId));
        stats.put("averageCompletionTime", participationRepository.calculateAverageCompletionTime(userId));
        stats.put("distinctCompletedActivities",
                participationRepository.countDistinctCompletedActivities(userId));

        return stats;
    }

    @Override
    public Map<String, Object> getGlobalStatistics() {
        Map<String, Object> stats = new HashMap<>();

        List<CognitiveActivity> allActivities = activityRepository.findAll();
        List<ActivityParticipation> allParticipations = participationRepository.findAll();

        stats.put("totalActivities", allActivities.size());
        stats.put("totalParticipations", allParticipations.size());
        stats.put("activeActivities",
                allActivities.stream().filter(CognitiveActivity::getIsActive).count());

        Map<String, Long> byType = allActivities.stream()
                .collect(Collectors.groupingBy(CognitiveActivity::getType, Collectors.counting()));
        stats.put("activitiesByType", byType);

        return stats;
    }
}