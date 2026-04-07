package esprit.edu.userservice1.services;

import esprit.edu.userservice1.entities.role;
import esprit.edu.userservice1.entities.user;
import esprit.edu.userservice1.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repo;

    @InjectMocks
    private UserServiceImpl userService;

    private user testUser;

    @BeforeEach
    void setUp() {
        testUser = new user();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setPassword("encoded_password");
        testUser.setFullName("Test User");
        testUser.setRole(role.USER);
        testUser.setBlocked(false);
    }

    // ══════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════

    @Test
    void create_ShouldSaveAndReturnUser_WhenEmailNotExists() {
        when(repo.existsByEmail("test@test.com")).thenReturn(false);
        when(repo.save(testUser)).thenReturn(testUser);

        user result = userService.create(testUser);

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
        verify(repo).save(testUser);
    }

    @Test
    void create_ShouldThrowRuntimeException_WhenEmailAlreadyExists() {
        when(repo.existsByEmail("test@test.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.create(testUser));

        assertEquals("Email already exists", ex.getMessage());
        verify(repo, never()).save(any());
    }

    // ══════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════

    @Test
    void getById_ShouldReturnUser_WhenFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));

        user result = userService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_ShouldThrowRuntimeException_WhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getById(99L));

        assertEquals("User not found", ex.getMessage());
    }

    // ══════════════════════════════════════════
    // GET BY EMAIL
    // ══════════════════════════════════════════

    @Test
    void getByEmail_ShouldReturnUser_WhenFound() {
        when(repo.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        user result = userService.getByEmail("test@test.com");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void getByEmail_ShouldThrowRuntimeException_WhenNotFound() {
        when(repo.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getByEmail("unknown@test.com"));

        assertEquals("User not found", ex.getMessage());
    }

    // ══════════════════════════════════════════
    // FIND BY EMAIL (sans exception)
    // ══════════════════════════════════════════

    @Test
    void findByEmail_ShouldReturnUser_WhenFound() {
        when(repo.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));

        user result = userService.findByEmail("test@test.com");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void findByEmail_ShouldReturnNull_WhenNotFound() {
        when(repo.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        user result = userService.findByEmail("nobody@test.com");

        assertNull(result);
    }

    // ══════════════════════════════════════════
    // GET ALL
    // ══════════════════════════════════════════

    @Test
    void getAll_ShouldReturnListOfUsers() {
        when(repo.findAll()).thenReturn(List.of(testUser));

        List<user> result = userService.getAll();

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).getEmail());
    }

    @Test
    void getAll_ShouldReturnEmptyList_WhenNoUsers() {
        when(repo.findAll()).thenReturn(List.of());

        List<user> result = userService.getAll();

        assertTrue(result.isEmpty());
    }

    // ══════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════

    @Test
    void update_ShouldUpdateFullName_WhenProvided() {
        user updated = new user();
        updated.setFullName("New Name");
        updated.setBlocked(false);

        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(repo.save(any())).thenReturn(testUser);

        user result = userService.update(1L, updated);

        assertEquals("New Name", result.getFullName());
        verify(repo).save(testUser);
    }

    @Test
    void update_ShouldUpdateBlockedStatus() {
        user updated = new user();
        updated.setBlocked(true);

        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(repo.save(any())).thenReturn(testUser);

        userService.update(1L, updated);

        assertTrue(testUser.isBlocked()); // ← champ toujours mis à jour
    }

    @Test
    void update_ShouldNotOverrideFields_WhenNull() {
        user updated = new user();
        updated.setFullName(null); // null → ne doit pas écraser
        updated.setBlocked(false);

        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(repo.save(any())).thenReturn(testUser);

        userService.update(1L, updated);

        assertEquals("Test User", testUser.getFullName()); // inchangé
    }

    @Test
    void update_ShouldThrow_WhenUserNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.update(99L, new user()));
    }

    // ══════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════

    @Test
    void delete_ShouldDeleteUser_WhenExists() {
        when(repo.existsById(1L)).thenReturn(true);
        doNothing().when(repo).deleteById(1L);

        assertDoesNotThrow(() -> userService.delete(1L));
        verify(repo).deleteById(1L);
    }

    @Test
    void delete_ShouldThrowRuntimeException_WhenNotFound() {
        when(repo.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.delete(99L));

        assertEquals("User not found", ex.getMessage());
        verify(repo, never()).deleteById(any());
    }

    // ══════════════════════════════════════════
    // UPDATE ROLE
    // ══════════════════════════════════════════

    @Test
    void updateRole_ShouldChangeRoleToAdmin() {
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));
        when(repo.save(any())).thenReturn(testUser);

        user result = userService.updateRole(1L, "ADMIN");

        assertEquals(role.ADMIN, result.getRole());
    }

    @Test
    void updateRole_ShouldThrow_WhenUserNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateRole(99L, "ADMIN"));

        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    void updateRole_ShouldThrow_WhenRoleInvalid() {
        when(repo.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateRole(1L, "INVALID_ROLE"));
    }

    // ══════════════════════════════════════════
    // FIND BY RESET TOKEN
    // ══════════════════════════════════════════

    @Test
    void findByResetToken_ShouldReturnUser_WhenTokenValid() {
        when(repo.findByResetToken("valid_token")).thenReturn(testUser);

        user result = userService.findByResetToken("valid_token");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void findByResetToken_ShouldReturnNull_WhenTokenNotFound() {
        when(repo.findByResetToken("bad_token")).thenReturn(null);

        user result = userService.findByResetToken("bad_token");

        assertNull(result);
    }
}