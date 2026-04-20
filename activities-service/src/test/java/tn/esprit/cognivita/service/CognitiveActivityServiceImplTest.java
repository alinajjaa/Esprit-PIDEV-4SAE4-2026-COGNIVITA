package tn.esprit.cognivita.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.cognivita.entity.CognitiveActivity;
import tn.esprit.cognivita.repository.CognitiveActivityRepository;
import tn.esprit.cognivita.repository.ActivityParticipationRepository;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CognitiveActivityServiceImplTest {

    @Mock
    private CognitiveActivityRepository repository;

    @SuppressWarnings("unused")
    @Mock
    private ActivityParticipationRepository participationRepository;

    @InjectMocks
    private tn.esprit.cognivita.service.impl.CognitiveActivityServiceImpl service;

    private CognitiveActivity sampleMemoryActivity;

    @BeforeEach
    void setUp() {
        sampleMemoryActivity = new CognitiveActivity();
        sampleMemoryActivity.setId(1L);
        sampleMemoryActivity.setTitle("Memory Test");
        sampleMemoryActivity.setType("MEMORY");
        sampleMemoryActivity.setDescription("Test memory activity");
        sampleMemoryActivity.setIsActive(true);
        sampleMemoryActivity.setWords(Arrays.asList("chat", "chien", "lapin"));
    }

    @Test
    @DisplayName("createActivity should save activity with words for MEMORY type")
    void createActivity_shouldSaveMemoryActivityWithWords() {
        when(repository.saveAndFlush(any(CognitiveActivity.class))).thenAnswer(invocation -> {
            CognitiveActivity a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        CognitiveActivity created = service.createActivity(sampleMemoryActivity);

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo(100L);
        assertThat(created.getWords()).containsExactly("chat", "chien", "lapin");
        verify(repository, times(1)).saveAndFlush(sampleMemoryActivity);
    }

    @Test
    @DisplayName("getActivityById should return activity when present")
    void getActivityById_whenPresent_returnsActivity() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleMemoryActivity));

        CognitiveActivity result = service.getActivityById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Memory Test");
        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("getActivityById should throw when not present")
    void getActivityById_whenNotPresent_throws() {
        when(repository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActivityById(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");

        verify(repository).findById(2L);
    }

    @Test
    @DisplayName("getAllActivities should return only active activities")
    void getAllActivities_shouldReturnOnlyActive() {
        CognitiveActivity inactive = new CognitiveActivity();
        inactive.setId(2L);
        inactive.setTitle("Inactive");
        inactive.setIsActive(false);

        when(repository.findByIsActiveTrue()).thenReturn(Collections.singletonList(sampleMemoryActivity));

        List<CognitiveActivity> result = service.getAllActivities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
        verify(repository).findByIsActiveTrue();
    }

    @Test
    @DisplayName("updateActivity should change fields and save")
    void updateActivity_shouldModifyAndSave() {
        CognitiveActivity updated = new CognitiveActivity();
        updated.setTitle("Memory Updated");
        updated.setDescription("Updated desc");
        updated.setType("MEMORY");
        updated.setWords(Arrays.asList("un", "deux", "trois"));

        when(repository.findById(1L)).thenReturn(Optional.of(sampleMemoryActivity));
        when(repository.save(any(CognitiveActivity.class))).thenAnswer(i -> i.getArgument(0));

        CognitiveActivity result = service.updateActivity(1L, updated);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Memory Updated");
        assertThat(result.getWords()).containsExactly("un", "deux", "trois");
        verify(repository).findById(1L);
        verify(repository).save(any(CognitiveActivity.class));
    }

    @Test
    @DisplayName("deleteActivity should delete when exists")
    void deleteActivity_shouldDeleteWhenExists() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleMemoryActivity));

        service.deleteActivity(1L);

        verify(repository).delete(sampleMemoryActivity);
    }

    @Test
    @DisplayName("getActivitiesByType should return matching activities")
    void getActivitiesByType_shouldReturnMatches() {
        when(repository.findByType("MEMORY")).thenReturn(Collections.singletonList(sampleMemoryActivity));

        List<CognitiveActivity> result = service.getActivitiesByType("MEMORY");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo("MEMORY");
        verify(repository).findByType("MEMORY");
    }

    @Test
    @DisplayName("getActivitiesByDifficulty should return matching activities")
    void getActivitiesByDifficulty_shouldReturnMatches() {
        sampleMemoryActivity.setDifficulty("MEDIUM");
        when(repository.findByDifficulty("MEDIUM")).thenReturn(Collections.singletonList(sampleMemoryActivity));

        List<CognitiveActivity> result = service.getActivitiesByDifficulty("MEDIUM");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDifficulty()).isEqualTo("MEDIUM");
        verify(repository).findByDifficulty("MEDIUM");
    }

    @Test
    @DisplayName("createActivity should allow less than 3 words (no validation implemented)")
    void createActivity_allowsLessThanThreeWords() {
        CognitiveActivity bad = new CognitiveActivity();
        bad.setType("MEMORY");
        bad.setWords(Arrays.asList("un", "deux"));

        when(repository.saveAndFlush(any(CognitiveActivity.class))).thenAnswer(i -> {
            CognitiveActivity a = i.getArgument(0);
            a.setId(200L);
            return a;
        });

        CognitiveActivity saved = service.createActivity(bad);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(200L);
        verify(repository).saveAndFlush(any(CognitiveActivity.class));
    }
}
