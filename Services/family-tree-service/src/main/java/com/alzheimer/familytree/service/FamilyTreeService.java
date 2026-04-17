package com.alzheimer.familytree.service;

import org.springframework.stereotype.Service;
import com.alzheimer.familytree.entity.FamilyMember;
import com.alzheimer.familytree.entity.FamilyGender;
import com.alzheimer.familytree.entity.Relationship;
import com.alzheimer.familytree.dto.FamilyTreeNode;
import com.alzheimer.familytree.repository.FamilyMemberRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for building the family tree and calculating hereditary Alzheimer's risk.
 *
 * FIX: getGlobalTreeStatistics() previously called
 *   repository.countByRelationshipForUser(null) which causes a JPQL exception
 *   (WHERE f.userId = null is always false / wrong SQL).
 *   Global stats now use in-memory stream grouping instead.
 */
@Service
public class FamilyTreeService {

    private final FamilyMemberRepository repository;

    public FamilyTreeService(FamilyMemberRepository repository) {
        this.repository = repository;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public FamilyMember addMember(FamilyMember member) {
        member.setHereditaryRiskScore(calculateMemberRiskContribution(member));
        return repository.save(member);
    }

    @Transactional
    public FamilyMember updateMember(Long id, FamilyMember updated) {
        FamilyMember existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Family member not found: " + id));
        existing.setFullName(updated.getFullName());
        existing.setRelationship(updated.getRelationship());
        existing.setAge(updated.getAge());
        existing.setIsAlive(updated.getIsAlive());
        existing.setHasAlzheimers(updated.getHasAlzheimers());
        existing.setHasDementia(updated.getHasDementia());
        existing.setGender(updated.getGender());
        existing.setOtherConditions(updated.getOtherConditions());
        existing.setNotes(updated.getNotes());
        existing.setParentMemberId(updated.getParentMemberId());
        existing.setHereditaryRiskScore(calculateMemberRiskContribution(existing));
        return repository.save(existing);
    }

    @Transactional
    public void deleteMember(Long id) {
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<FamilyMember> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<FamilyMember> getAllMembers(Long userId) {
        return repository.findByUserId(userId);
    }

    // ── Tree Builder (BFS) ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FamilyTreeNode> buildTree(Long userId) {
        List<FamilyMember> allMembers = repository.findByUserId(userId);

        Map<Long, FamilyTreeNode> nodeMap = new LinkedHashMap<>();
        for (FamilyMember m : allMembers) {
            nodeMap.put(m.getId(), new FamilyTreeNode(m));
        }

        List<FamilyTreeNode> roots = new ArrayList<>();
        for (FamilyTreeNode node : nodeMap.values()) {
            if (node.getParentMemberId() == null || !nodeMap.containsKey(node.getParentMemberId())) {
                roots.add(node);
            } else {
                nodeMap.get(node.getParentMemberId()).getChildren().add(node);
            }
        }
        return roots;
    }

    // ── Risk Analysis ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> calculateHereditaryRisk(Long userId) {
        List<FamilyMember> allMembers  = repository.findByUserId(userId);
        List<FamilyMember> firstDegree = repository.findFirstDegreeRelatives(userId);
        List<FamilyMember> grandparents= repository.findGrandparents(userId);

        double totalRisk = 0;
        List<String> riskPatterns = new ArrayList<>();
        Map<String, Long> affectedByRelationship = new LinkedHashMap<>();

        long firstDegreeAffected = firstDegree.stream()
                .filter(m -> Boolean.TRUE.equals(m.getHasAlzheimers())).count();
        if (firstDegreeAffected > 0) {
            totalRisk += firstDegreeAffected * 15.0;
            riskPatterns.add(firstDegreeAffected + " first-degree relative(s) diagnosed with Alzheimer's");
        }

        long grandparentsAffected = grandparents.stream()
                .filter(m -> Boolean.TRUE.equals(m.getHasAlzheimers())).count();
        if (grandparentsAffected > 0) {
            totalRisk += grandparentsAffected * 7.5;
            riskPatterns.add(grandparentsAffected + " grandparent(s) with Alzheimer's history");
        }

        Set<Long> tier1Ids = new HashSet<>();
        firstDegree.forEach(m -> tier1Ids.add(m.getId()));
        grandparents.forEach(m -> tier1Ids.add(m.getId()));

        long otherAffected = allMembers.stream()
                .filter(m -> Boolean.TRUE.equals(m.getHasAlzheimers()))
                .filter(m -> !tier1Ids.contains(m.getId()))
                .count();
        if (otherAffected > 0) {
            totalRisk += otherAffected * 3.0;
            riskPatterns.add(otherAffected + " extended relative(s) with Alzheimer's");
        }

        long dementiaAffected = allMembers.stream()
                .filter(m -> Boolean.TRUE.equals(m.getHasDementia())).count();
        if (dementiaAffected > 0) {
            totalRisk += dementiaAffected * 4.0;
            riskPatterns.add(dementiaAffected + " relative(s) with dementia history");
        }

        boolean multiGenerational = detectMultiGenerationalPattern(firstDegree, grandparents);
        if (multiGenerational) {
            totalRisk += 10;
            riskPatterns.add("Multi-generational Alzheimer's pattern detected");
        }

        for (FamilyMember m : allMembers) {
            if (Boolean.TRUE.equals(m.getHasAlzheimers()) || Boolean.TRUE.equals(m.getHasDementia())) {
                String rel = m.getRelationship() != null ? m.getRelationship().name() : "OTHER";
                affectedByRelationship.merge(rel, 1L, Long::sum);
            }
        }

        double finalRisk = Math.min(100, Math.round(totalRisk * 10.0) / 10.0);
        String riskLevel = finalRisk < 20 ? "LOW" : finalRisk < 40 ? "MODERATE"
                : finalRisk < 60 ? "HIGH" : "VERY_HIGH";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hereditaryRiskScore",    finalRisk);
        result.put("riskLevel",              riskLevel);
        result.put("riskPatterns",           riskPatterns);
        result.put("totalFamilyMembers",     allMembers.size());
        result.put("affectedMembers",        allMembers.stream().filter(m ->
                Boolean.TRUE.equals(m.getHasAlzheimers()) || Boolean.TRUE.equals(m.getHasDementia())).count());
        result.put("firstDegreeAffected",    firstDegreeAffected);
        result.put("grandparentsAffected",   grandparentsAffected);
        result.put("affectedByRelationship", affectedByRelationship);
        result.put("multiGenerational",      multiGenerational);
        return result;
    }

    private boolean detectMultiGenerationalPattern(List<FamilyMember> firstDegree,
                                                    List<FamilyMember> grandparents) {
        boolean fd = firstDegree.stream().anyMatch(m -> Boolean.TRUE.equals(m.getHasAlzheimers()));
        boolean gp = grandparents.stream().anyMatch(m -> Boolean.TRUE.equals(m.getHasAlzheimers()));
        return fd && gp;
    }

    // ── Per-user statistics ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getTreeStatistics(Long userId) {
        long total        = repository.countByUserId(userId);
        long alzheimers   = repository.countAlzheimersAffectedByUserId(userId);
        long dementia     = repository.countDementiaAffectedByUserId(userId);
        long anyCondition = repository.countAnyConditionByUserId(userId);
        long living       = repository.countLivingMembersByUserId(userId);
        long deceased     = repository.countDeceasedMembersByUserId(userId);
        Double avgRisk    = repository.avgHereditaryRiskScore(userId);
        Double maxRisk    = repository.maxHereditaryRiskScore(userId);

        List<Object[]> byRelationship = repository.countByRelationshipForUser(userId);
        Map<String, Long> relMap = new LinkedHashMap<>();
        for (Object[] row : byRelationship) relMap.put(row[0].toString(), (Long) row[1]);

        List<Object[]> byGender = repository.countByGenderForUser(userId);
        Map<String, Long> genderMap = new LinkedHashMap<>();
        for (Object[] row : byGender)
            genderMap.put(row[0] != null ? row[0].toString() : "UNKNOWN", (Long) row[1]);

        List<Object[]> alzByRel = repository.countAlzheimersAffectedByRelationship(userId);
        Map<String, Long> alzRelMap = new LinkedHashMap<>();
        for (Object[] row : alzByRel) alzRelMap.put(row[0].toString(), (Long) row[1]);

        List<Object[]> demByRel = repository.countDementiaAffectedByRelationship(userId);
        Map<String, Long> demRelMap = new LinkedHashMap<>();
        for (Object[] row : demByRel) demRelMap.put(row[0].toString(), (Long) row[1]);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMembers",               total);
        stats.put("livingMembers",              living);
        stats.put("deceasedMembers",            deceased);
        stats.put("affectedWithAlzheimers",     alzheimers);
        stats.put("affectedWithDementia",       dementia);
        stats.put("affectedWithAnyCondition",   anyCondition);
        stats.put("averageHereditaryRiskScore", avgRisk != null ? Math.round(avgRisk * 10.0) / 10.0 : 0.0);
        stats.put("maxHereditaryRiskScore",     maxRisk != null ? Math.round(maxRisk * 10.0) / 10.0 : 0.0);
        stats.put("membersByRelationship",      relMap);
        stats.put("membersByGender",            genderMap);
        stats.put("alzheimersByRelationship",   alzRelMap);
        stats.put("dementiaByRelationship",     demRelMap);
        return stats;
    }

    // ── Global statistics (FIX: no longer calls JPQL with null userId) ────────

    @Transactional(readOnly = true)
    public Map<String, Object> getGlobalTreeStatistics() {
        // FIX: load all members once and group in-memory.
        // Old code called repository.countByRelationshipForUser(null) which produced
        // "WHERE f.userId = null" — always returns 0 rows and breaks JPQL.
        List<FamilyMember> all = repository.findAll();

        long totalMembers   = all.size();
        long totalAlzheimers= all.stream().filter(m -> Boolean.TRUE.equals(m.getHasAlzheimers())).count();
        long totalDementia  = all.stream().filter(m -> Boolean.TRUE.equals(m.getHasDementia())).count();
        long totalAffected  = all.stream().filter(m ->
                Boolean.TRUE.equals(m.getHasAlzheimers()) || Boolean.TRUE.equals(m.getHasDementia())).count();

        Map<String, Long> relMap = all.stream()
                .filter(m -> m.getRelationship() != null)
                .collect(Collectors.groupingBy(m -> m.getRelationship().name(), Collectors.counting()));

        Map<String, Long> alzRelMap = all.stream()
                .filter(m -> Boolean.TRUE.equals(m.getHasAlzheimers()) && m.getRelationship() != null)
                .collect(Collectors.groupingBy(m -> m.getRelationship().name(), Collectors.counting()));

        double avgRisk = all.stream()
                .filter(m -> m.getHereditaryRiskScore() != null && m.getHereditaryRiskScore() > 0)
                .mapToDouble(FamilyMember::getHereditaryRiskScore)
                .average().orElse(0.0);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalFamilyMembers",             totalMembers);
        stats.put("totalAffectedWithAlzheimers",    totalAlzheimers);
        stats.put("totalAffectedWithDementia",      totalDementia);
        stats.put("totalAffectedWithAnyCondition",  totalAffected);
        stats.put("alzheimersPrevalence",           totalMembers > 0
                ? Math.round((totalAlzheimers * 100.0 / totalMembers) * 10.0) / 10.0 : 0.0);
        stats.put("averageHereditaryRiskScore",     Math.round(avgRisk * 10.0) / 10.0);
        stats.put("membersByRelationship",          relMap);
        stats.put("alzheimersByRelationship",       alzRelMap);
        return stats;
    }

    // ── Per-member risk contribution ──────────────────────────────────────────

    private double calculateMemberRiskContribution(FamilyMember member) {
        if (!Boolean.TRUE.equals(member.getHasAlzheimers())
                && !Boolean.TRUE.equals(member.getHasDementia())) {
            return 0.0;
        }
        double base = Boolean.TRUE.equals(member.getHasAlzheimers()) ? 20 : 10;
        if (member.getRelationship() == null) return base;
        return switch (member.getRelationship()) {
            case FATHER, MOTHER -> base * 1.5;
            case PATERNAL_GRANDFATHER, PATERNAL_GRANDMOTHER,
                 MATERNAL_GRANDFATHER, MATERNAL_GRANDMOTHER -> base * 1.2;
            case BROTHER, SISTER -> base * 1.3;
            default -> base;
        };
    }
}
