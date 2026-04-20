package com.alzheimer.healthprevention.service;

import com.alzheimer.healthprevention.dto.HealthProfileDTO;
import com.alzheimer.healthprevention.entity.*;
import com.alzheimer.healthprevention.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HealthProfileService Unit Tests")
class HealthProfileServiceTest {

    @Mock private HealthProfileRepository profileRepository;
    @Mock private HealthRecommendationRepository recommendationRepository;
    @Mock private WellnessActivityRepository activityRepository;

    @InjectMocks
    private HealthProfileService service;

    private HealthProfile existingProfile;
    private HealthProfileDTO dto;

    @BeforeEach
    void setUp() {
        existingProfile = new HealthProfile();
        existingProfile.setId(1L);
        existingProfile.setUserId(42L);
        existingProfile.setPhysicalActivityLevel(ActivityLevel.MODERATELY_ACTIVE);
        existingProfile.setSleepHoursPerNight(7.5);
        existingProfile.setDietQuality(DietQuality.GOOD);
        existingProfile.setStressLevel(StressLevel.MODERATE);
        existingProfile.setSmokingStatus(false);
        existingProfile.setSocialEngagementLevel(EngagementLevel.MODERATE);

        dto = new HealthProfileDTO();
        dto.setUserId(42L);
        dto.setPhysicalActivityLevel(ActivityLevel.MODERATELY_ACTIVE);
        dto.setSleepHoursPerNight(7.5);
        dto.setDietQuality(DietQuality.GOOD);
        dto.setStressLevel(StressLevel.MODERATE);
        dto.setSmokingStatus(false);
        dto.setSocialEngagementLevel(EngagementLevel.MODERATE);
    }

    // ── createProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("createProfile: saves and returns new profile")
    void createProfile_savesNewProfile() {
        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(profileRepository.save(any())).thenAnswer(i -> {
            HealthProfile p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        HealthProfile result = service.createProfile(dto);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(42L);
        verify(profileRepository).save(any());
    }

    @Test
    @DisplayName("createProfile: throws when profile already exists for user")
    void createProfile_throwsWhenAlreadyExists() {
        when(profileRepository.existsByUserId(42L)).thenReturn(true);

        assertThatThrownBy(() -> service.createProfile(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    // ── getProfileByUserId ────────────────────────────────────────────────────

    @Test
    @DisplayName("getProfileByUserId: returns Optional with profile when present")
    void getProfileByUserId_returnsProfile() {
        when(profileRepository.findByUserId(42L)).thenReturn(Optional.of(existingProfile));

        Optional<HealthProfile> result = service.getProfileByUserId(42L);

        assertThat(result).isPresent();
        assertThat(result.get().getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getProfileByUserId: returns empty Optional when not found")
    void getProfileByUserId_returnsEmpty() {
        when(profileRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThat(service.getProfileByUserId(99L)).isEmpty();
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile: modifies fields and saves")
    void updateProfile_modifiesAndSaves() {
        dto.setDietQuality(DietQuality.EXCELLENT);
        dto.setStressLevel(StressLevel.MINIMAL);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(existingProfile));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HealthProfile result = service.updateProfile(1L, dto);

        assertThat(result.getDietQuality()).isEqualTo(DietQuality.EXCELLENT);
        assertThat(result.getStressLevel()).isEqualTo(StressLevel.MINIMAL);
        verify(profileRepository).save(any());
    }

    @Test
    @DisplayName("updateProfile: throws when profile not found")
    void updateProfile_throwsWhenNotFound() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfile(99L, dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("not found");
    }

    // ── deleteProfile ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteProfile: calls deleteById when profile exists")
    void deleteProfile_callsDeleteById() {
        when(profileRepository.existsById(1L)).thenReturn(true);

        service.deleteProfile(1L);

        verify(profileRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteProfile: throws when profile not found")
    void deleteProfile_throwsWhenNotFound() {
        when(profileRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteProfile(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── Wellness Score (calculateWellnessScore) ───────────────────────────────

    @Test
    @DisplayName("createProfile: EXTREMELY_ACTIVE + optimal sleep + EXCELLENT diet = high wellness score")
    void wellnessScore_bestLifestyle_highScore() {
        dto.setPhysicalActivityLevel(ActivityLevel.EXTREMELY_ACTIVE);
        dto.setSleepHoursPerNight(8.0);
        dto.setDietQuality(DietQuality.EXCELLENT);
        dto.setStressLevel(StressLevel.MINIMAL);
        dto.setSmokingStatus(false);
        dto.setSocialEngagementLevel(EngagementLevel.VERY_HIGH);

        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HealthProfile result = service.createProfile(dto);

        assertThat(result.getWellnessScore()).isGreaterThan(80.0);
    }

    @Test
    @DisplayName("createProfile: SEDENTARY + poor sleep + poor diet + smoking = low wellness score")
    void wellnessScore_worstLifestyle_lowScore() {
        dto.setPhysicalActivityLevel(ActivityLevel.SEDENTARY);
        dto.setSleepHoursPerNight(4.0);
        dto.setDietQuality(DietQuality.POOR);
        dto.setStressLevel(StressLevel.VERY_HIGH);
        dto.setSmokingStatus(true);
        dto.setSocialEngagementLevel(EngagementLevel.ISOLATED);

        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HealthProfile result = service.createProfile(dto);

        assertThat(result.getWellnessScore()).isLessThan(40.0);
    }

    @Test
    @DisplayName("wellness score: is never negative")
    void wellnessScore_neverNegative() {
        dto.setPhysicalActivityLevel(ActivityLevel.SEDENTARY);
        dto.setDietQuality(DietQuality.POOR);
        dto.setStressLevel(StressLevel.VERY_HIGH);
        dto.setSmokingStatus(true);

        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HealthProfile result = service.createProfile(dto);

        assertThat(result.getWellnessScore()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    @DisplayName("wellness score: is never above 100")
    void wellnessScore_neverAbove100() {
        dto.setPhysicalActivityLevel(ActivityLevel.EXTREMELY_ACTIVE);
        dto.setSleepHoursPerNight(8.0);
        dto.setDietQuality(DietQuality.EXCELLENT);
        dto.setStressLevel(StressLevel.MINIMAL);
        dto.setSmokingStatus(false);
        dto.setSocialEngagementLevel(EngagementLevel.VERY_HIGH);

        when(profileRepository.existsByUserId(42L)).thenReturn(false);
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HealthProfile result = service.createProfile(dto);

        assertThat(result.getWellnessScore()).isLessThanOrEqualTo(100.0);
    }

    // ── generateRecommendations ───────────────────────────────────────────────

    @Test
    @DisplayName("generateRecommendations: generates PHYSICAL_ACTIVITY rec for SEDENTARY profile")
    void generateRecommendations_sedentaryGetsPhysicalActivityRec() {
        existingProfile.setPhysicalActivityLevel(ActivityLevel.SEDENTARY);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(existingProfile));
        when(recommendationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<HealthRecommendation> recs = service.generateRecommendations(1L);

        assertThat(recs.stream().anyMatch(r ->
                r.getCategory() == RecommendationCategory.PHYSICAL_ACTIVITY)).isTrue();
    }

    @Test
    @DisplayName("generateRecommendations: smoker gets LIFESTYLE_CHANGE CRITICAL recommendation")
    void generateRecommendations_smokerGetsCriticalRec() {
        existingProfile.setSmokingStatus(true);

        when(profileRepository.findById(1L)).thenReturn(Optional.of(existingProfile));
        when(recommendationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<HealthRecommendation> recs = service.generateRecommendations(1L);

        assertThat(recs.stream().anyMatch(r ->
                r.getCategory() == RecommendationCategory.LIFESTYLE_CHANGE &&
                r.getPriority() == Priority.CRITICAL)).isTrue();
    }

    @Test
    @DisplayName("generateRecommendations: always includes annual cognitive assessment rec")
    void generateRecommendations_alwaysIncludesMedicalCheckup() {
        when(profileRepository.findById(1L)).thenReturn(Optional.of(existingProfile));
        when(recommendationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<HealthRecommendation> recs = service.generateRecommendations(1L);

        assertThat(recs.stream().anyMatch(r ->
                r.getCategory() == RecommendationCategory.MEDICAL_CHECKUP)).isTrue();
    }

    @Test
    @DisplayName("generateRecommendations: throws when profile not found")
    void generateRecommendations_throwsWhenNotFound() {
        when(profileRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateRecommendations(99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
