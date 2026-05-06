package com.cognivita.dashboardservice.repository;

import com.cognivita.dashboardservice.dto.MmseMonthlySeverityDto;
import com.cognivita.dashboardservice.entity.MMSETest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MmseAnalyticsRepository extends JpaRepository<MMSETest, Long> {

    @Query("""
        SELECT new com.cognivita.dashboardservice.dto.MmseMonthlySeverityDto(
            CONCAT(YEAR(t.testDate), '-', MONTH(t.testDate)),
            COUNT(t),
            SUM(CASE WHEN t.totalScore >= 24 THEN 1 ELSE 0 END),
            SUM(CASE WHEN t.totalScore BETWEEN 18 AND 23 THEN 1 ELSE 0 END),
            SUM(CASE WHEN t.totalScore BETWEEN 10 AND 17 THEN 1 ELSE 0 END),
            SUM(CASE WHEN t.totalScore < 10 THEN 1 ELSE 0 END),
            AVG(t.totalScore)
        )
        FROM MMSETest t
        WHERE t.testDate BETWEEN :fromDate AND :toDate
        GROUP BY YEAR(t.testDate), MONTH(t.testDate)
        ORDER BY YEAR(t.testDate), MONTH(t.testDate)
        """)
    List<MmseMonthlySeverityDto> getMmseMonthlySeverity(@Param("fromDate") LocalDate fromDate,
                                                        @Param("toDate") LocalDate toDate);
}
