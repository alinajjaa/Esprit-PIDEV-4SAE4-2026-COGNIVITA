package com.alzheimer.medicalrecords.repository;
import com.alzheimer.medicalrecords.repository.*;

import com.alzheimer.medicalrecords.entity.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType, Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.occurredAt >= :since ORDER BY a.occurredAt DESC")
    List<AuditLog> findRecent(@Param("since") LocalDateTime since);

    @Query("SELECT a FROM AuditLog a ORDER BY a.occurredAt DESC")
    Page<AuditLog> findAllPaged(Pageable pageable);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.occurredAt >= :since GROUP BY a.action")
    List<Object[]> countByActionSince(@Param("since") LocalDateTime since);
}
