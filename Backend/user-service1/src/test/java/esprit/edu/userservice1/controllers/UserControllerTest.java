package esprit.edu.userservice1.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import esprit.edu.userservice1.dto.AuthResponse;
import esprit.edu.userservice1.entities.role;
import esprit.edu.userservice1.entities.user;
import esprit.edu.userservice1.repositories.UserRepository;
import esprit.edu.userservice1.security.JwtService;
import esprit.edu.userservice1.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserService        userService;
    @MockBean private JwtService         jwtService;
    @MockBean private PasswordEncoder    passwordEncoder;
    @MockBean private EmailService       emailService;
    @MockBean private UserRepository     userRepository;
    @MockBean private TwoFaService       twoFaService;
    @MockBean private CloudinaryService  cloudinaryService;
    @MockBean private FaceAuthService    faceAuthService;

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
        testUser.setEmailVerified(true);
    }

    // ══════════════════════════════════════════
    // FACE SERVICE STATUS
    // ══════════════════════════════════════════

    @Test
    void faceServiceStatus_ShouldReturnAlive_WhenPythonRunning() throws Exception {
        when(faceAuthService.isPythonServiceAlive()).thenReturn(true);

        mockMvc.perform(get("/api/users/face-service-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pythonServiceAlive").value(true));
    }

    @Test
    void faceServiceStatus_ShouldReturnDown_WhenPythonOff() throws Exception {
        when(faceAuthService.isPythonServiceAlive()).thenReturn(false);

        mockMvc.perform(get("/api/users/face-service-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pythonServiceAlive").value(false));
    }

    // ══════════════════════════════════════════
    // REGISTER
    // ══════════════════════════════════════════

    @Test
    void register_ShouldReturn200_WhenValidRequest() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userService.create(any())).thenReturn(testUser);
        when(twoFaService.sendOtp(anyString())).thenReturn(true);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "password": "pass1234",
                  "firstName": "Test",
                  "lastName": "User"
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void register_ShouldReturn400_WhenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "password": "pass1234"
                }
            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "password": "pass1234"
                }
            """))
                .andExpect(status().isConflict());
    }

    // ══════════════════════════════════════════
    // VERIFY REGISTER OTP
    // ══════════════════════════════════════════

    @Test
    void verifyRegister_ShouldReturnFaceRequired_WhenOtpValid() throws Exception {
        when(twoFaService.verifyOtpForRegister("test@test.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        mockMvc.perform(post("/api/users/verify-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "otp": "123456"
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("face_required"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void verifyRegister_ShouldReturn400_WhenOtpInvalid() throws Exception {
        when(twoFaService.verifyOtpForRegister("test@test.com", "000000")).thenReturn(false);

        mockMvc.perform(post("/api/users/verify-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "otp": "000000"
                }
            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_otp"));
    }

    @Test
    void verifyRegister_ShouldReturn400_WhenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/users/verify-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "otp": "123456"
                }
            """))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════
    // LOGIN
    // ══════════════════════════════════════════

    @Test
    void login_ShouldReturnFaceRequired_WhenCredentialsValid() throws Exception {
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("pass1234", "encoded_password")).thenReturn(true);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "password": "pass1234"
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.step").value("face_required"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void login_ShouldReturn401_WhenPasswordWrong() throws Exception {
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpass", "encoded_password")).thenReturn(false);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "password": "wrongpass"
                }
            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturn401_WhenUserNotFound() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "unknown@test.com",
                  "password": "pass1234"
                }
            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_ShouldReturn403_WhenUserBlocked() throws Exception {
        testUser.setBlocked(true);
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "password": "pass1234"
                }
            """))
                .andExpect(status().isForbidden());
    }

    // ══════════════════════════════════════════
    // VERIFY FACE
    // ══════════════════════════════════════════

    @Test
    void verifyFace_ShouldReturnToken_WhenFaceMatches() throws Exception {
        Map<String, Object> faceResult = Map.of(
                "match", true,
                "score", 0.95,
                "confidence_level", "HIGH"
        );
        when(faceAuthService.verifyFace(eq(1L), anyList())).thenReturn(faceResult);
        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);
        when(jwtService.generateToken("test@test.com", "USER")).thenReturn("jwt_token");

        mockMvc.perform(post("/api/users/verify-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "userId": 1,
                  "embedding": [0.1, 0.2, 0.3]
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"))
                .andExpect(jsonPath("$.confidence_level").value("HIGH"));
    }

    @Test
    void verifyFace_ShouldReturn401_WhenFaceNotMatch() throws Exception {
        Map<String, Object> faceResult = Map.of(
                "match", false,
                "score", 0.2,
                "confidence_level", "REFUSED"
        );
        when(faceAuthService.verifyFace(eq(1L), anyList())).thenReturn(faceResult);

        mockMvc.perform(post("/api/users/verify-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "userId": 1,
                  "embedding": [0.9, 0.8, 0.7]
                }
            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("face_mismatch"));
    }

    @Test
    void verifyFace_ShouldReturn400_WhenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/users/verify-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════
    // REGISTER FACE
    // ══════════════════════════════════════════

    @Test
    void registerFace_ShouldReturnToken_WhenSuccess() throws Exception {
        when(faceAuthService.registerFace(eq(1L), anyList())).thenReturn(true);
        when(faceAuthService.verifyFace(eq(1L), anyList())).thenReturn(Map.of(
                "match", true, "score", 99.0, "confidence_level", "HIGH"
        ));
        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);
        when(jwtService.generateToken("test@test.com", "USER")).thenReturn("jwt_token");

        mockMvc.perform(post("/api/users/register-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "userId": 1,
                  "embedding": [0.1, 0.2, 0.3]
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"));
    }

    @Test
    void registerFace_ShouldReturn500_WhenPythonFails() throws Exception {
        when(faceAuthService.registerFace(eq(1L), anyList())).thenReturn(false);

        mockMvc.perform(post("/api/users/register-face")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "userId": 1,
                  "embedding": [0.1, 0.2, 0.3]
                }
            """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ══════════════════════════════════════════
    // VERIFY OTP LOGIN
    // ══════════════════════════════════════════

    @Test
    void verifyOtp_ShouldReturnToken_WhenValid() throws Exception {
        when(twoFaService.verifyOtpForLogin("test@test.com", "123456")).thenReturn(true);
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);
        when(jwtService.generateToken("test@test.com", "USER")).thenReturn("jwt_token");

        mockMvc.perform(post("/api/users/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "otp": "123456"
                }
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token"));
    }

    @Test
    void verifyOtp_ShouldReturn400_WhenInvalid() throws Exception {
        when(twoFaService.verifyOtpForLogin("test@test.com", "000000")).thenReturn(false);

        mockMvc.perform(post("/api/users/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "email": "test@test.com",
                  "otp": "000000"
                }
            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_otp"));
    }

    // ══════════════════════════════════════════
    // RESEND OTP
    // ══════════════════════════════════════════

    @Test
    void resendOtp_ShouldReturn200_WhenSent() throws Exception {
        when(twoFaService.sendOtp("test@test.com")).thenReturn(true);

        mockMvc.perform(post("/api/users/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"test@test.com\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP resent successfully"));
    }

    @Test
    void resendOtp_ShouldReturn404_WhenUserNotFound() throws Exception {
        when(twoFaService.sendOtp("unknown@test.com")).thenReturn(false);

        mockMvc.perform(post("/api/users/resend-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"unknown@test.com\" }"))
                .andExpect(status().isNotFound());
    }

    // ══════════════════════════════════════════
    // CRUD
    // ══════════════════════════════════════════

    @Test
    void getAll_ShouldReturnList() throws Exception {
        when(userService.getAll()).thenReturn(List.of(testUser));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"));
    }

    @Test
    void getById_ShouldReturnUser() throws Exception {
        when(userService.getById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void getByEmail_ShouldReturnUser() throws Exception {
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);

        mockMvc.perform(get("/api/users/by-email")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void delete_ShouldReturn200() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk());
    }

    // ══════════════════════════════════════════
    // BLOCK / UNBLOCK
    // ══════════════════════════════════════════

    @Test
    void blockUser_ShouldReturn200() throws Exception {
        testUser.setBlocked(true);
        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1/block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));
    }

    @Test
    void unblockUser_ShouldReturn200() throws Exception {
        testUser.setBlocked(false);
        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1/unblock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(false));
    }

    // ══════════════════════════════════════════
    // UPLOAD PHOTO
    // ══════════════════════════════════════════

    @Test
    void uploadPhoto_ShouldReturnUrl_WhenSuccess() throws Exception {
        when(cloudinaryService.uploadImage(any())).thenReturn("https://cloudinary.com/photo.jpg");
        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/users/1/uploadPhoto").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("https://cloudinary.com/photo.jpg"));
    }

    @Test
    void uploadPhoto_ShouldReturn400_WhenFileEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "", "image/jpeg", new byte[0]
        );

        mockMvc.perform(multipart("/api/users/1/uploadPhoto").file(emptyFile))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════
    // PASSWORD RESET
    // ══════════════════════════════════════════

    @Test
    void forgotPassword_ShouldReturn200_Always() throws Exception {
        when(userService.findByEmail("test@test.com")).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);
        doNothing().when(emailService).sendResetEmail(anyString(), anyString());

        mockMvc.perform(post("/api/users/forgot-password")
                        .param("email", "test@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_ShouldReturn200_WhenTokenValid() throws Exception {
        testUser.setResetToken("valid_token");
        testUser.setResetTokenExpiry(Instant.now().plusSeconds(300));
        when(userService.findByResetToken("valid_token")).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("new_encoded");
        when(userService.update(eq(1L), any())).thenReturn(testUser);

        mockMvc.perform(post("/api/users/reset-password")
                        .param("token", "valid_token")
                        .param("newPassword", "NewPass123"))
                .andExpect(status().isOk())
                .andExpect(content().string("Password updated successfully"));
    }

    @Test
    void resetPassword_ShouldReturn400_WhenTokenExpired() throws Exception {
        testUser.setResetToken("expired_token");
        testUser.setResetTokenExpiry(Instant.now().minusSeconds(100));
        when(userService.findByResetToken("expired_token")).thenReturn(testUser);

        mockMvc.perform(post("/api/users/reset-password")
                        .param("token", "expired_token")
                        .param("newPassword", "NewPass123"))
                .andExpect(status().isBadRequest());
    }

    // ══════════════════════════════════════════
    // UPDATE ROLE
    // ══════════════════════════════════════════

    @Test
    void updateRole_ShouldReturn200() throws Exception {
        testUser.setRole(role.ADMIN);
        when(userService.updateRole(1L, "ADMIN")).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1/role")
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ══════════════════════════════════════════
    // UPDATE EMOTION
    // ══════════════════════════════════════════

    @Test
    void updateEmotion_ShouldReturn200() throws Exception {
        testUser.setLastEmotion("happy");
        when(userService.getById(1L)).thenReturn(testUser);
        when(userService.update(eq(1L), any())).thenReturn(testUser);

        mockMvc.perform(put("/api/users/1/emotion")
                        .param("emotion", "happy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastEmotion").value("happy"));
    }

    @Test
    void updateEmotion_ShouldReturn404_WhenUserNotFound() throws Exception {
        when(userService.getById(99L)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );

        mockMvc.perform(put("/api/users/99/emotion")
                        .param("emotion", "sad"))
                .andExpect(status().isNotFound());
    }
}