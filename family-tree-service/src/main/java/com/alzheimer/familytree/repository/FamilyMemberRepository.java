package com.alzheimer.familytree.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import com.alzheimer.familytree.entity.FamilyMember;
import com.alzheimer.familytree.entity.Relationship;

public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    @Query("SELECT f FROM FamilyMember f WHERE f.userId = :userId ORDER BY f.relationship ASC")
    List<FamilyMember> findByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM FamilyMember f WHERE f.userId = :userId AND f.hasAlzheimers = true")
    List<FamilyMember> findAlzheimersAffectedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FamilyMember f WHERE f.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND f.hasAlzheimers = true")
    Long countAlzheimersAffectedByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND f.hasDementia = true")
    Long countDementiaAffectedByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM FamilyMember f WHERE f.userId = :userId AND f.relationship IN " +
           "('FATHER','MOTHER','BROTHER','SISTER') ORDER BY f.relationship ASC")
    List<FamilyMember> findFirstDegreeRelatives(@Param("userId") Long userId);

    @Query("SELECT f FROM FamilyMember f WHERE f.userId = :userId AND f.relationship IN " +
           "('PATERNAL_GRANDFATHER','PATERNAL_GRANDMOTHER','MATERNAL_GRANDFATHER','MATERNAL_GRANDMOTHER') " +
           "ORDER BY f.relationship ASC")
    List<FamilyMember> findGrandparents(@Param("userId") Long userId);

    @Query("SELECT f FROM FamilyMember f WHERE f.userId = :userId AND f.parentMemberId = :parentId")
    List<FamilyMember> findByParentMemberId(@Param("userId") Long userId, @Param("parentId") Long parentId);

    @Query("SELECT f FROM FamilyMember f WHERE f.userId = :userId AND " +
           "(f.hasAlzheimers = true OR f.hasDementia = true) ORDER BY f.relationship ASC")
    List<FamilyMember> findAffectedMembers(@Param("userId") Long userId);

    // Statistics JPQL queries
    @Query("SELECT f.relationship, COUNT(f) FROM FamilyMember f WHERE f.userId = :userId GROUP BY f.relationship")
    List<Object[]> countByRelationshipForUser(@Param("userId") Long userId);

    @Query("SELECT AVG(f.hereditaryRiskScore) FROM FamilyMember f WHERE f.userId = :userId AND f.hereditaryRiskScore > 0")
    Double avgHereditaryRiskScore(@Param("userId") Long userId);

    @Query("SELECT f.gender, COUNT(f) FROM FamilyMember f WHERE f.userId = :userId GROUP BY f.gender")
    List<Object[]> countByGenderForUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND f.isAlive = true")
    Long countLivingMembersByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND f.isAlive = false")
    Long countDeceasedMembersByUserId(@Param("userId") Long userId);

    @Query("SELECT f.relationship, COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND f.hasAlzheimers = true GROUP BY f.relationship")
    List<Object[]> countAlzheimersAffectedByRelationship(@Param("userId") Long userId);

    @Query("SELECT f.relationship, COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND f.hasDementia = true GROUP BY f.relationship")
    List<Object[]> countDementiaAffectedByRelationship(@Param("userId") Long userId);

    @Query("SELECT MAX(f.hereditaryRiskScore) FROM FamilyMember f WHERE f.userId = :userId")
    Double maxHereditaryRiskScore(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM FamilyMember f WHERE f.userId = :userId AND (f.hasAlzheimers = true OR f.hasDementia = true)")
    Long countAnyConditionByUserId(@Param("userId") Long userId);
}
