package com.cognivita.dashboardservice.repository;

import com.cognivita.dashboardservice.dto.AppointmentComplianceDto;
import com.cognivita.dashboardservice.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentAnalyticsRepository extends JpaRepository<Appointment, Long> {

    @Query("""
        SELECT new com.cognivita.dashboardservice.dto.AppointmentComplianceDto(
            m.riskLevel,
            a.appointmentType,
            COUNT(a),
            SUM(CASE WHEN a.status = com.cognivita.dashboardservice.enums.AppointmentStatus.COMPLETED THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.status = com.cognivita.dashboardservice.enums.AppointmentStatus.MISSED THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.status = com.cognivita.dashboardservice.enums.AppointmentStatus.CANCELLED THEN 1 ELSE 0 END),
            AVG(
                CASE
                    WHEN a.completedAt IS NOT NULL
                    THEN timestampdiff(HOUR, a.scheduledAt, a.completedAt)
                    ELSE NULL
                END
            )
        )
        FROM Appointment a, MedicalRecord m
        WHERE m.id = a.medicalRecordId
          AND a.scheduledAt BETWEEN :from AND :to
        GROUP BY m.riskLevel, a.appointmentType
        """)
    List<AppointmentComplianceDto> getAppointmentCompliance(@Param("from") LocalDateTime from,
                                                            @Param("to") LocalDateTime to);
}
