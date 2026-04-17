package com.cognivita.dashboardservice.repository;

import com.cognivita.dashboardservice.dto.AlertTypePressureDto;
import com.cognivita.dashboardservice.dto.AlertTrendDto;
import com.cognivita.dashboardservice.dto.PatientSafetyBurdenDto;
import com.cognivita.dashboardservice.entity.TrackingAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TrackingAlertAnalyticsRepository extends JpaRepository<TrackingAlert, Long> {

    @Query("""
        SELECT new com.cognivita.dashboardservice.dto.AlertTypePressureDto(
            a.type,
            COUNT(a),
            SUM(CASE WHEN a.read = false THEN 1 ELSE 0 END),
            SUM(CASE
                WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.OUT_OF_ZONE THEN 2
                WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.NO_MOVEMENT THEN 1
                ELSE 0 END),
            AVG(timestampdiff(HOUR, a.createdAt, :to))
        )
        FROM TrackingAlert a
        WHERE a.createdAt BETWEEN :from AND :to
        GROUP BY a.type
        """)
    List<AlertTypePressureDto> getAlertTypePressure(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.cognivita.dashboardservice.dto.PatientSafetyBurdenDto(
            a.patientId,
            COUNT(a),
            SUM(CASE WHEN a.read = false THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.NO_MOVEMENT THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.OUT_OF_ZONE THEN 1 ELSE 0 END),
            SUM(CASE
                WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.OUT_OF_ZONE THEN 2
                WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.NO_MOVEMENT THEN 1
                ELSE 0 END)
        )
        FROM TrackingAlert a
        WHERE a.createdAt BETWEEN :from AND :to
        GROUP BY a.patientId
        ORDER BY SUM(CASE
            WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.OUT_OF_ZONE THEN 2
            WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.NO_MOVEMENT THEN 1
            ELSE 0 END) DESC
        """)
    List<PatientSafetyBurdenDto> getPatientSafetyRanking(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    @Query("""
        SELECT new com.cognivita.dashboardservice.dto.AlertTrendDto(
            CONCAT(YEAR(a.createdAt), '-', MONTH(a.createdAt), '-', DAY(a.createdAt)),
            COUNT(a),
            SUM(CASE WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.OUT_OF_ZONE THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.type = com.cognivita.dashboardservice.enums.AlertType.NO_MOVEMENT THEN 1 ELSE 0 END)
        )
        FROM TrackingAlert a
        WHERE a.createdAt BETWEEN :from AND :to
        GROUP BY YEAR(a.createdAt), MONTH(a.createdAt), DAY(a.createdAt)
        ORDER BY YEAR(a.createdAt), MONTH(a.createdAt), DAY(a.createdAt)
        """)
    List<AlertTrendDto> getAlertTrend(@Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to);
}
