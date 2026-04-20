package tn.esprit.cognivita.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tn.esprit.cognivita.entity.CognitiveActivity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class CognitiveActivityRepositoryTest {

    @Autowired
    private CognitiveActivityRepository repository;

    @Test
    @DisplayName("save should persist activity and generate id")
    void save_shouldPersist() {
        CognitiveActivity a = new CognitiveActivity();
        a.setTitle("Repo Test");
        a.setType("MEMORY");
        a.setIsActive(true);
        a.setWords(Arrays.asList("x","y","z"));

        CognitiveActivity saved = repository.save(a);

        assertThat(saved.getId()).isNotNull();
        assertThat(repository.findById(saved.getId())).isPresent();
    }

    @Test
    @DisplayName("findByType should return matching activities")
    void findByType_returnsMatches() {
        CognitiveActivity a = new CognitiveActivity();
        a.setTitle("Repo Type");
        a.setType("STROOP");
        a.setIsActive(true);
        repository.save(a);

        List<CognitiveActivity> found = repository.findByType("STROOP");
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getType()).isEqualTo("STROOP");
    }

    @Test
    @DisplayName("findByIsActiveTrue should return active activities")
    void findByIsActiveTrue_returnsActive() {
        CognitiveActivity active = new CognitiveActivity();
        active.setTitle("Active");
        active.setIsActive(true);
        repository.save(active);

        List<CognitiveActivity> found = repository.findByIsActiveTrue();
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("searchByTitleOrDescription should find activities by keyword")
    void searchByTitleOrDescription_findsByKeyword() {
        CognitiveActivity a = new CognitiveActivity();
        a.setTitle("Lovely chat activity");
        a.setDescription("Contains the word chat in description");
        a.setIsActive(true);
        repository.save(a);

        List<CognitiveActivity> found = repository.searchByTitleOrDescription("chat");
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getTitle().toLowerCase()).contains("chat");
    }
}
