package com.alzheimer.backend.mmse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MMSEService Unit Tests")
class MMSEServiceTest {

    @Mock
    private MMSERepository repository;

    @InjectMocks
    private MMSEService service;

    private MMSETest sampleTest;

    @BeforeEach
    void setUp() {
        sampleTest = new MMSETest();
        sampleTest.setId(1L);
        sampleTest.setPatientName("John Doe");
        sampleTest.setOrientationScore(8);
        sampleTest.setRegistrationScore(3);
        sampleTest.setAttentionScore(4);
        sampleTest.setRecallScore(2);
        sampleTest.setLanguageScore(7);
        sampleTest.setTestDate(LocalDate.of(2026, 4, 17));
    }

    @Test
    @DisplayName("save should persist MMSETest and return it")
    void save_persistsTest() {
        when(repository.save(sampleTest)).thenReturn(sampleTest);

        MMSETest result = service.save(sampleTest);

        assertThat(result).isNotNull();
        assertThat(result.getPatientName()).isEqualTo("John Doe");
        verify(repository).save(sampleTest);
    }

    @Test
    @DisplayName("findAll should return all MMSE tests")
    void findAll_returnsAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(sampleTest));

        List<MMSETest> result = service.findAll();

        assertThat(result).hasSize(1);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("findById should return test when present")
    void findById_whenPresent_returnsTest() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleTest));

        MMSETest result = service.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getPatientName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("findById should throw RuntimeException when not found")
    void findById_whenNotFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MMSE Test not found");
    }

    @Test
    @DisplayName("update should modify all fields and save")
    void update_modifiesAndSaves() {
        MMSETest updated = new MMSETest();
        updated.setPatientName("Jane Doe");
        updated.setOrientationScore(10);
        updated.setRegistrationScore(3);
        updated.setAttentionScore(5);
        updated.setRecallScore(3);
        updated.setLanguageScore(9);
        updated.setNotes("Improved significantly");

        when(repository.findById(1L)).thenReturn(Optional.of(sampleTest));
        when(repository.save(any(MMSETest.class))).thenAnswer(i -> i.getArgument(0));

        MMSETest result = service.update(1L, updated);

        assertThat(result.getPatientName()).isEqualTo("Jane Doe");
        assertThat(result.getOrientationScore()).isEqualTo(10);
        assertThat(result.getNotes()).isEqualTo("Improved significantly");
        verify(repository).save(any(MMSETest.class));
    }

    @Test
    @DisplayName("update should throw when MMSE test not found")
    void update_whenNotFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        MMSETest any = new MMSETest();
        assertThatThrownBy(() -> service.update(99L, any))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("delete should call repository deleteById")
    void delete_callsDeleteById() {
        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    // ── MMSETest entity logic tests ──────────────────────────────────────────

    @Test
    @DisplayName("calculateScore: perfect score = 30, Normal cognition")
    void calculateScore_perfectScore_normalCognition() {
        MMSETest test = new MMSETest();
        test.setOrientationScore(10);
        test.setRegistrationScore(3);
        test.setAttentionScore(5);
        test.setRecallScore(3);
        test.setLanguageScore(9);
        test.calculateScore();

        assertThat(test.getTotalScore()).isEqualTo(30);
        assertThat(test.getInterpretation()).isEqualTo("Normal cognition");
    }

    @Test
    @DisplayName("calculateScore: score 20 = Mild cognitive impairment")
    void calculateScore_score20_mildImpairment() {
        MMSETest test = new MMSETest();
        test.setOrientationScore(7);
        test.setRegistrationScore(3);
        test.setAttentionScore(3);
        test.setRecallScore(2);
        test.setLanguageScore(5);
        test.calculateScore();

        assertThat(test.getTotalScore()).isEqualTo(20);
        assertThat(test.getInterpretation()).isEqualTo("Mild cognitive impairment");
    }

    @Test
    @DisplayName("calculateScore: score 14 = Moderate cognitive impairment")
    void calculateScore_score14_moderateImpairment() {
        MMSETest test = new MMSETest();
        test.setOrientationScore(5);
        test.setRegistrationScore(2);
        test.setAttentionScore(2);
        test.setRecallScore(1);
        test.setLanguageScore(4);
        test.calculateScore();

        assertThat(test.getTotalScore()).isEqualTo(14);
        assertThat(test.getInterpretation()).isEqualTo("Moderate cognitive impairment");
    }

    @Test
    @DisplayName("calculateScore: score 5 = Severe cognitive impairment")
    void calculateScore_score5_severeImpairment() {
        MMSETest test = new MMSETest();
        test.setOrientationScore(2);
        test.setRegistrationScore(1);
        test.setAttentionScore(1);
        test.setRecallScore(0);
        test.setLanguageScore(1);
        test.calculateScore();

        assertThat(test.getTotalScore()).isEqualTo(5);
        assertThat(test.getInterpretation()).isEqualTo("Severe cognitive impairment");
    }

    @Test
    @DisplayName("calculateScore: testDate defaults to today when null")
    void calculateScore_testDateDefaultsToToday() {
        MMSETest test = new MMSETest();
        test.calculateScore();

        assertThat(test.getTestDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("calculateScore: existing testDate is preserved")
    void calculateScore_existingDatePreserved() {
        LocalDate fixed = LocalDate.of(2025, 1, 15);
        MMSETest test = new MMSETest();
        test.setTestDate(fixed);
        test.calculateScore();

        assertThat(test.getTestDate()).isEqualTo(fixed);
    }
}
