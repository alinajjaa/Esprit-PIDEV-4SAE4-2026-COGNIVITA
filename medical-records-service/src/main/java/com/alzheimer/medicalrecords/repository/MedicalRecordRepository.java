package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    // ── Paginated list (2-phase: count+IDs via Pageable, then JOIN FETCH by IDs) ──
    // Using JOIN FETCH directly with Pageable causes Hibernate to load everything
    // into memory and paginate in Java. Instead, we page over IDs, then hydrate.

    @Query("SELECT m FROM MedicalRecord m JOIN FETCH m.user ORDER BY m.createdAt DESC")
    List<MedicalRecord> findAllWithUser();

    /** Phase-1: get a page of IDs only */
    @Query("SELECT m.id FROM MedicalRecord m")
    Page<Long> findAllIds(Pageable pageable);

    @Query("SELECT m.id FROM MedicalRecord m WHERE m.riskLevel = :rl")
    Page<Long> findIdsByRiskLevel(@Param("rl") RiskLevel rl, Pageable pageable);

    @Query("SELECT m.id FROM MedicalRecord m WHERE m.gender = :g")
    Page<Long> findIdsByGender(@Param("g") Gender g, Pageable pageable);

    @Query("SELECT m.id FROM MedicalRecord m WHERE m.familyHistory = :fh")
    Page<Long> findIdsByFamilyHistory(@Param("fh") FamilyHistory fh, Pageable pageable);

    /** Phase-2: fetch full objects by IDs with JOIN FETCH (no pagination here) */
    @Query("SELECT m FROM MedicalRecord m JOIN FETCH m.user WHERE m.id IN :ids ORDER BY m.createdAt DESC")
    List<MedicalRecord> findByIdsWithUser(@Param("ids") List<Long> ids);

    // ── Single record ────────────────────────────────────────────────────────

    @Query("SELECT m FROM MedicalRecord m LEFT JOIN FETCH m.user WHERE m.id = :id")
    Optional<MedicalRecord> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT m FROM MedicalRecord m JOIN FETCH m.user WHERE m.user.id = :userId")
    List<MedicalRecord> findByUserIdWithUser(@Param("userId") Long userId);

    @Query("SELECT m FROM MedicalRecord m LEFT JOIN FETCH m.user WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<MedicalRecord> findTopByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // ── Stats ────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(m) FROM MedicalRecord m WHERE m.user.id = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM MedicalRecord m")
    Long countAllRecords();

    @Query("SELECT AVG(m.riskScore) FROM MedicalRecord m")
    Double averageRiskScore();

    @Query("SELECT m.id, m.riskScore, m.riskLevel FROM MedicalRecord m WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    List<Object[]> findLatestRiskScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT m.riskLevel, COUNT(m) FROM MedicalRecord m GROUP BY m.riskLevel")
    List<Object[]> countByRiskLevel();

    @Query("SELECT m.gender, COUNT(m) FROM MedicalRecord m GROUP BY m.gender")
    List<Object[]> countByGender();

    @Query("SELECT m.familyHistory, COUNT(m) FROM MedicalRecord m GROUP BY m.familyHistory")
    List<Object[]> countByFamilyHistoryJPQL();

    /** Returns the most recent record for a user — used by adherence consumer */
    @Query("SELECT m FROM MedicalRecord m JOIN FETCH m.user WHERE m.user.id = :userId ORDER BY m.createdAt DESC")
    java.util.Optional<MedicalRecord> findByUserId(@Param("userId") Long userId);
}
