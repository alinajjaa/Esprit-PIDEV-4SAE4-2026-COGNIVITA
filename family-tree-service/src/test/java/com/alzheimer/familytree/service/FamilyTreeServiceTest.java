package com.alzheimer.familytree.service;

import com.alzheimer.familytree.dto.FamilyTreeNode;
import com.alzheimer.familytree.entity.FamilyGender;
import com.alzheimer.familytree.entity.FamilyMember;
import com.alzheimer.familytree.entity.Relationship;
import com.alzheimer.familytree.repository.FamilyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyTreeService Unit Tests")
class FamilyTreeServiceTest {

    @Mock
    private FamilyMemberRepository repository;

    @InjectMocks
    private FamilyTreeService service;

    private FamilyMember father;
    private FamilyMember mother;

    @BeforeEach
    void setUp() {
        father = makeMember(1L, 1L, "Ahmed Ben Ali", Relationship.FATHER, true, false, 65);
        mother = makeMember(2L, 1L, "Fatma Ben Ali", Relationship.MOTHER, true, false, 62);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addMember: healthy member gets 0 risk score")
    void addMember_healthyMember_zeroRisk() {
        when(repository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

        FamilyMember result = service.addMember(father);

        assertThat(result.getHereditaryRiskScore()).isEqualTo(0.0);
        verify(repository).save(father);
    }

    @Test
    @DisplayName("addMember: father with Alzheimer's gets FATHER multiplier (20 * 1.5 = 30)")
    void addMember_fatherWithAlzheimers_riskScore30() {
        father.setHasAlzheimers(true);
        when(repository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

        FamilyMember result = service.addMember(father);

        assertThat(result.getHereditaryRiskScore()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("addMember: mother with dementia only gets 10 * 1.5 = 15")
    void addMember_motherDementiaOnly_riskScore15() {
        mother.setHasDementia(true);
        when(repository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

        FamilyMember result = service.addMember(mother);

        assertThat(result.getHereditaryRiskScore()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("addMember: brother with Alzheimer's gets 20 * 1.3 = 26")
    void addMember_brotherWithAlzheimers_riskScore26() {
        FamilyMember brother = makeMember(3L, 1L, "Karim", Relationship.BROTHER, true, false, 40);
        brother.setHasAlzheimers(true);
        when(repository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

        FamilyMember result = service.addMember(brother);

        assertThat(result.getHereditaryRiskScore()).isEqualTo(26.0);
    }

    @Test
    @DisplayName("getAllMembers should return members for a given userId")
    void getAllMembers_returnsForUser() {
        when(repository.findByUserId(1L)).thenReturn(Arrays.asList(father, mother));

        List<FamilyMember> result = service.getAllMembers(1L);

        assertThat(result).hasSize(2);
        verify(repository).findByUserId(1L);
    }

    @Test
    @DisplayName("updateMember should change fields and recalculate risk")
    void updateMember_changesFieldsAndRisk() {
        FamilyMember updated = makeMember(1L, 1L, "Ahmed Updated", Relationship.FATHER, true, false, 66);
        updated.setHasAlzheimers(true);

        when(repository.findById(1L)).thenReturn(Optional.of(father));
        when(repository.save(any(FamilyMember.class))).thenAnswer(i -> i.getArgument(0));

        FamilyMember result = service.updateMember(1L, updated);

        assertThat(result.getFullName()).isEqualTo("Ahmed Updated");
        assertThat(result.getHereditaryRiskScore()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("updateMember should throw when member not found")
    void updateMember_whenNotFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMember(99L, father))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Family member not found");
    }

    @Test
    @DisplayName("deleteMember should call repository deleteById")
    void deleteMember_callsDeleteById() {
        service.deleteMember(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    @DisplayName("findById should return Optional with member when present")
    void findById_whenPresent_returnsMember() {
        when(repository.findById(1L)).thenReturn(Optional.of(father));

        Optional<FamilyMember> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Ahmed Ben Ali");
    }

    // ── Tree Builder ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildTree: orphan nodes become roots")
    void buildTree_orphanNodesAreRoots() {
        when(repository.findByUserId(1L)).thenReturn(Arrays.asList(father, mother));

        List<FamilyTreeNode> roots = service.buildTree(1L);

        assertThat(roots).hasSize(2);
    }

    @Test
    @DisplayName("buildTree: child is nested under parent node")
    void buildTree_childNestedUnderParent() {
        FamilyMember child = makeMember(10L, 1L, "Sara", Relationship.SISTER, true, false, 30);
        child.setParentMemberId(1L);

        when(repository.findByUserId(1L)).thenReturn(Arrays.asList(father, child));

        List<FamilyTreeNode> roots = service.buildTree(1L);

        assertThat(roots).hasSize(1);
        assertThat(roots.get(0).getChildren()).hasSize(1);
        assertThat(roots.get(0).getChildren().get(0).getFullName()).isEqualTo("Sara");
    }

    @Test
    @DisplayName("buildTree: returns empty list when no members")
    void buildTree_emptyWhenNoMembers() {
        when(repository.findByUserId(1L)).thenReturn(Collections.emptyList());

        List<FamilyTreeNode> roots = service.buildTree(1L);

        assertThat(roots).isEmpty();
    }

    // ── Hereditary Risk ───────────────────────────────────────────────────────

    @Test
    @DisplayName("calculateHereditaryRisk: one first-degree with Alzheimer's = 15 pts")
    void calculateHereditaryRisk_oneFirstDegreeAlzheimers_15pts() {
        father.setHasAlzheimers(true);

        when(repository.findByUserId(1L)).thenReturn(Arrays.asList(father));
        when(repository.findFirstDegreeRelatives(1L)).thenReturn(Arrays.asList(father));
        when(repository.findGrandparents(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.calculateHereditaryRisk(1L);

        assertThat((Double) result.get("hereditaryRiskScore")).isEqualTo(15.0);
        assertThat(result.get("riskLevel")).isEqualTo("LOW");
        assertThat((Long) result.get("firstDegreeAffected")).isEqualTo(1L);
    }

    @Test
    @DisplayName("calculateHereditaryRisk: no affected relatives = 0 pts, LOW risk")
    void calculateHereditaryRisk_noAffected_zeroRiskLow() {
        when(repository.findByUserId(1L)).thenReturn(Arrays.asList(father, mother));
        when(repository.findFirstDegreeRelatives(1L)).thenReturn(Arrays.asList(father, mother));
        when(repository.findGrandparents(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.calculateHereditaryRisk(1L);

        assertThat((Double) result.get("hereditaryRiskScore")).isEqualTo(0.0);
        assertThat(result.get("riskLevel")).isEqualTo("LOW");
    }

    @Test
    @DisplayName("calculateHereditaryRisk: score capped at 100")
    void calculateHereditaryRisk_cappedAt100() {
        List<FamilyMember> many = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            FamilyMember m = makeMember((long) i, 1L, "Relative" + i, Relationship.FATHER, true, false, 70);
            m.setHasAlzheimers(true);
            many.add(m);
        }

        when(repository.findByUserId(1L)).thenReturn(many);
        when(repository.findFirstDegreeRelatives(1L)).thenReturn(many);
        when(repository.findGrandparents(1L)).thenReturn(Collections.emptyList());

        Map<String, Object> result = service.calculateHereditaryRisk(1L);

        assertThat((Double) result.get("hereditaryRiskScore")).isLessThanOrEqualTo(100.0);
    }

    // ── Global stats ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getGlobalTreeStatistics: counts affected members correctly")
    void getGlobalTreeStatistics_countsCorrectly() {
        father.setHasAlzheimers(true);
        when(repository.findAll()).thenReturn(Arrays.asList(father, mother));

        Map<String, Object> stats = service.getGlobalTreeStatistics();

        assertThat((Long) stats.get("totalFamilyMembers")).isEqualTo(2L);
        assertThat((Long) stats.get("totalAffectedWithAlzheimers")).isEqualTo(1L);
        assertThat((Long) stats.get("totalAffectedWithDementia")).isEqualTo(0L);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private FamilyMember makeMember(Long id, Long userId, String name,
                                    Relationship rel, boolean alive, boolean alzheimers, int age) {
        FamilyMember m = new FamilyMember();
        m.setId(id);
        m.setUserId(userId);
        m.setFullName(name);
        m.setRelationship(rel);
        m.setIsAlive(alive);
        m.setHasAlzheimers(alzheimers);
        m.setHasDementia(false);
        m.setAge(age);
        m.setGender(FamilyGender.MALE);
        m.setHereditaryRiskScore(0.0);
        return m;
    }
}
