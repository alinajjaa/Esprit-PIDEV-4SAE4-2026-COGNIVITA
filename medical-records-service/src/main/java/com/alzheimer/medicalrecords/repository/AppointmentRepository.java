package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("SELECT a FROM Appointment a WHERE a.medicalRecordId = :recordId ORDER BY a.scheduledAt ASC")
    List<Appointment> findByMedicalRecordId(@Param("recordId") Long recordId);

    @Query("SELECT a FROM Appointment a WHERE a.medicalRecordId = :recordId AND a.status = :status ORDER BY a.scheduledAt ASC")
    List<Appointment> findByMedicalRecordIdAndStatus(@Param("recordId") Long recordId,
                                                     @Param("status") AppointmentStatus status);

    /** Appointments within the reminder window: scheduled in the next 48 hours, not yet reminded, not cancelled */
    @Query("""
        SELECT a FROM Appointment a
        JOIN FETCH a.medicalRecord mr
        JOIN FETCH mr.user u
        WHERE a.status = 'SCHEDULED'
          AND a.reminderSent = false
          AND a.scheduledAt BETWEEN :now AND :window
        """)
    List<Appointment> findUpcomingForReminder(@Param("now") LocalDateTime now,
                                              @Param("window") LocalDateTime window);

    /** Appointments that are still SCHEDULED but their time has passed — mark MISSED */
    @Query("SELECT a FROM Appointment a WHERE a.status = 'SCHEDULED' AND a.scheduledAt < :now")
    List<Appointment> findOverdueScheduled(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.medicalRecordId = :recordId")
    Long countByMedicalRecordId(@Param("recordId") Long recordId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.medicalRecordId = :recordId AND a.status = :status")
    Long countByMedicalRecordIdAndStatus(@Param("recordId") Long recordId,
                                         @Param("status") AppointmentStatus status);
}