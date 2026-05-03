package esprit.edu.userservice1.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email", unique = true)
        }
)
public class user {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fullName CAN be null (first/last name optional)
    @Column(length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    // Never serialize password
    @JsonIgnore
    @Column(nullable = true, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private role role;

    @Column(length = 500)
    private String photoUrl;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(length = 255)
    private String resetToken;

    private Instant resetTokenExpiry;
    // ✅ Blocage
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean blocked = false;
    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
    // 2FA champs
    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "email_verified", columnDefinition = "boolean default true")
    private Boolean emailVerified = true;

    @Column(name = "last_2fa_verified_at")
    private LocalDateTime last2faVerifiedAt;
    @Column(name = "last_emotion")
    private String lastEmotion;

    @Column(name = "face_confidence")
    private Float faceConfidence;

    @Column(name = "face_confidence_level")
    private String faceConfidenceLevel;
    /* ========= Getters & Setters ========= */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public role getRole() { return role; }
    public void setRole(role role) { this.role = role; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public Instant getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(Instant resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }

    // ── Getters / Setters ──────────────────────────────
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public LocalDateTime getLast2faVerifiedAt() { return last2faVerifiedAt; }
    public void setLast2faVerifiedAt(LocalDateTime last2faVerifiedAt) { this.last2faVerifiedAt = last2faVerifiedAt; }
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    public String getLastEmotion()              { return lastEmotion; }
    public void setLastEmotion(String emotion)  { this.lastEmotion = emotion; }
    public Float getFaceConfidence()                        { return faceConfidence; }
    public void setFaceConfidence(Float faceConfidence)     { this.faceConfidence = faceConfidence; }
    public String getFaceConfidenceLevel()                  { return faceConfidenceLevel; }
    public void setFaceConfidenceLevel(String level)        { this.faceConfidenceLevel = level; }

}
