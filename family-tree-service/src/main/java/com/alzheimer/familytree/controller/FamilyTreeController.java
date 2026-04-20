package com.alzheimer.familytree.controller;

import com.alzheimer.familytree.dto.ApiResponse;
import com.alzheimer.familytree.entity.FamilyMember;
import com.alzheimer.familytree.dto.FamilyTreeNode;
import com.alzheimer.familytree.service.FamilyTreeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/family-tree")
public class FamilyTreeController {

    private final FamilyTreeService service;

    public FamilyTreeController(FamilyTreeService service) {
        this.service = service;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<FamilyMember>>> getMembers(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Family members retrieved",
                    service.getAllMembers(userId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve family members", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/tree")
    public ResponseEntity<ApiResponse<List<FamilyTreeNode>>> getTree(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Family tree built",
                    service.buildTree(userId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to build family tree", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/risk-analysis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRiskAnalysis(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Hereditary risk analysis complete",
                    service.calculateHereditaryRisk(userId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to calculate hereditary risk", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Statistics retrieved",
                    service.getTreeStatistics(userId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve stats", e.getMessage()));
        }
    }

    @GetMapping("/global-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGlobalStats() {
        try {
            return ResponseEntity.ok(ApiResponse.success("Global stats retrieved",
                    service.getGlobalTreeStatistics()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve global stats", e.getMessage()));
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<FamilyMember>> addMember(@RequestBody FamilyMember member) {
        try {
            FamilyMember saved = service.addMember(member);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Family member added", saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to add family member", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<FamilyMember>> updateMember(
            @PathVariable Long id, @RequestBody FamilyMember member) {
        try {
            FamilyMember updated = service.updateMember(id, member);
            return ResponseEntity.ok(ApiResponse.success("Family member updated", updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Family member not found", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to update family member", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        try {
            service.deleteMember(id);
            return ResponseEntity.ok(ApiResponse.success("Family member deleted", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete family member", e.getMessage()));
        }
    }

}
