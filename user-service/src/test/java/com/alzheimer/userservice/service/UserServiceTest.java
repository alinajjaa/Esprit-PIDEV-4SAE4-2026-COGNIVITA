package com.alzheimer.userservice.service;

import com.alzheimer.userservice.entity.User;
import com.alzheimer.userservice.entity.UserRole;
import com.alzheimer.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("alice@example.com");
        sampleUser.setFirstName("Alice");
        sampleUser.setLastName("Smith");
        sampleUser.setPassword("secret");
        sampleUser.setPhone("+21612345678");
        sampleUser.setRole(UserRole.PATIENT);
        sampleUser.setActive(true);
    }

    @Test
    @DisplayName("save should persist user and return it")
    void save_persistsUser() {
        when(repository.save(sampleUser)).thenReturn(sampleUser);

        User result = service.save(sampleUser);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        verify(repository).save(sampleUser);
    }

    @Test
    @DisplayName("findAll should return all users")
    void findAll_returnsAll() {
        when(repository.findAll()).thenReturn(Arrays.asList(sampleUser));

        List<User> result = service.findAll();

        assertThat(result).hasSize(1);
        verify(repository).findAll();
    }

    @Test
    @DisplayName("findById should return Optional with user when present")
    void findById_whenPresent_returnsUser() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleUser));

        Optional<User> result = service.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("findById should return empty Optional when absent")
    void findById_whenAbsent_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> result = service.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByEmail should return user when found")
    void findByEmail_whenFound_returnsUser() {
        when(repository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));

        User result = service.findByEmail("alice@example.com");

        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("findByEmail should throw when email not found")
    void findByEmail_whenNotFound_throws() {
        when(repository.findByEmail("unknown@x.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByEmail("unknown@x.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("findActiveUsers should delegate to repository")
    void findActiveUsers_delegatesToRepository() {
        when(repository.findByActive(true)).thenReturn(Collections.singletonList(sampleUser));

        List<User> result = service.findActiveUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActive()).isTrue();
        verify(repository).findByActive(true);
    }

    @Test
    @DisplayName("findUsersByRole should return users with given role")
    void findUsersByRole_returnsMatchingUsers() {
        when(repository.findByRole(UserRole.PATIENT)).thenReturn(Collections.singletonList(sampleUser));

        List<User> result = service.findUsersByRole(UserRole.PATIENT);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.PATIENT);
    }

    @Test
    @DisplayName("update should modify all fields and save")
    void update_modifiesAndSaves() {
        User updated = new User();
        updated.setEmail("bob@example.com");
        updated.setFirstName("Bob");
        updated.setLastName("Jones");
        updated.setPassword("newpass");
        updated.setPhone("+21698765432");
        updated.setRole(UserRole.DOCTOR);
        updated.setActive(true);

        when(repository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(repository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = service.update(1L, updated);

        assertThat(result.getEmail()).isEqualTo("bob@example.com");
        assertThat(result.getFirstName()).isEqualTo("Bob");
        assertThat(result.getRole()).isEqualTo(UserRole.DOCTOR);
        verify(repository).save(any(User.class));
    }

    @Test
    @DisplayName("update should throw RuntimeException when user not found")
    void update_whenNotFound_throws() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, sampleUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("delete should call repository deleteById")
    void delete_callsDeleteById() {
        service.delete(1L);

        verify(repository).deleteById(1L);
    }
}
