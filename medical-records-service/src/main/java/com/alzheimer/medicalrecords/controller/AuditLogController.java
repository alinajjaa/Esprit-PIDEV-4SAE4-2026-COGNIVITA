package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import com.alzheimer.medicalrecords.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /** GET /api/audit?page=0&size=20  — paginated audit log */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<AuditLog> p = auditLogRepository.findAllPaged(
                    PageRequest.of(page, size, Sort.by("occurredAt").descending()));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content",       p.getContent());
            result.put("totalElements", p.getTotalElements());
            result.put("totalPages",    p.getTotalPages());
            return ResponseEntity.ok(ApiResponse.success("Audit log", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    /** GET /api/audit/entity/{type}/{id}  — audit trail for one entity */
    @GetMapping("/entity/{entityType}/{entityId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<AuditLog>>> getForEntity(
            @PathVariable String entityType, @PathVariable Long entityId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Entity audit trail",
                    auditLogRepository.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(entityType, entityId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }

    /** GET /api/audit/stats  — last-7-day action breakdown */
    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(7);
            List<Object[]> rows = auditLogRepository.countByActionSince(since);
            Map<String, Long> byAction = new LinkedHashMap<>();
            for (Object[] row : rows) byAction.put(row[0].toString(), (Long) row[1]);
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("last7Days", byAction);
            stats.put("totalLast7Days", byAction.values().stream().mapToLong(Long::longValue).sum());
            return ResponseEntity.ok(ApiResponse.success("Audit stats", stats));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed", e.getMessage()));
        }
    }
}
