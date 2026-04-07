package esprit.edu.userservice1.controllers;

import esprit.edu.userservice1.dto.AuthResponse;
import esprit.edu.userservice1.dto.LoginRequest;
import esprit.edu.userservice1.dto.RegisterRequest;
import esprit.edu.userservice1.dto.UpdateUserRequest;
import esprit.edu.userservice1.entities.role;
import esprit.edu.userservice1.entities.user;
import esprit.edu.userservice1.repositories.UserRepository;
import esprit.edu.userservice1.security.JwtService;
import esprit.edu.userservice1.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:4200")
public class UserController {

    private final UserService service;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TwoFaService twoFaService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private FaceAuthService faceAuthService;


    public UserController(UserService service, JwtService jwtService,
                          PasswordEncoder passwordEncoder, EmailService emailService) {
        this.service = service;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /* ══════════════════════════════════════════
       REGISTER
       ══════════════════════════════════════════ */
    @GetMapping("/face-service-status")
    public ResponseEntity<?> faceServiceStatus() {
        boolean alive = faceAuthService.isPythonServiceAlive();
        return ResponseEntity.ok(Map.of(
                "pythonServiceAlive", alive,
                "message", alive
                        ? "✅ Face service is running"
                        : "❌ Face service is DOWN — start app.py on port 5001"
        ));
    }
    @PostMapping
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.email() == null || req.email().isBlank() ||
                req.password() == null || req.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required");
        }

        user existing = service.findByEmail(req.email());
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        user u = new user();
        u.setEmail(req.email());
        u.setPassword(passwordEncoder.encode(req.password()));

        String fullName = ((req.firstName() == null) ? "" : req.firstName().trim()) +
                " " +
                ((req.lastName() == null) ? "" : req.lastName().trim());
        u.setFullName(fullName.trim().isEmpty() ? null : fullName.trim());
        u.setPhotoUrl(req.photoUrl());
        u.setRole(role.valueOf("USER"));
        u.setEmailVerified(false);

        user saved = service.create(u);

        // ❌ SUPPRIMER tout ce bloc — le visage n'existe pas encore ici
        // if (req.embedding() != null && !req.embedding().isEmpty()) {
        //     faceAuthService.registerFace(saved.getId(), req.embedding());
        // }

        twoFaService.sendOtp(saved.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "OTP sent to your email. Please verify your account.",
                "email", saved.getEmail()
        ));
    }
    /* ══════════════════════════════════════════
       VERIFY REGISTER OTP
       ══════════════════════════════════════════ */

    @PostMapping("/verify-register")
    public ResponseEntity<?> verifyRegister(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body("Email and OTP are required");
        }

        if (!twoFaService.verifyOtpForRegister(email, otp)) {
            return ResponseEntity.status(400).body(Map.of(
                    "error", "invalid_otp",
                    "message", "Invalid or expired OTP"
            ));
        }

        user u = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ AJOUTER — marquer email comme vérifié
        u.setEmailVerified(true);
        userRepository.save(u);

        return ResponseEntity.ok(Map.of(
                "step",   "face_required",
                "userId", u.getId()
        ));
    }

    /* ══════════════════════════════════════════
       LOGIN
       ══════════════════════════════════════════ */

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        user u = service.findByEmail(req.email().trim());

        // ✅ Vérifier credentials
        if (u == null || u.getPassword() == null || u.getPassword().isBlank() ||
                !passwordEncoder.matches(req.password(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        // ✅ Vérifier si bloqué
        if (u.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account has been blocked. Please contact support.");
        }

        // ✅ Étape 1 — Credentials OK → demander Face ID
        return ResponseEntity.ok(Map.of(
                "step", "face_required",
                "userId", u.getId(),
                "message", "Credentials verified. Please complete face authentication."
        ));
    }



    /* ══════════════════════════════════════════
   VERIFY FACE — LOGIN étape 2
   ══════════════════════════════════════════ */
    @PostMapping("/verify-face")
    public ResponseEntity<?> verifyFace(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("userId") == null || body.get("embedding") == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "userId and embedding are required"
                ));
            }

            Long userId = Long.valueOf(body.get("userId").toString());
            List<Double> embedding = (List<Double>) body.get("embedding");

            if (embedding.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "embedding is empty"
                ));
            }

            // ✅ Récupérer résultat complet depuis Python
            Map<String, Object> result = faceAuthService.verifyFace(userId, embedding);
            boolean match = Boolean.TRUE.equals(result.get("match"));

            if (!match) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "error",            "face_mismatch",
                        "message",          "Face not recognized ❌",
                        "score",            result.getOrDefault("score", 0),
                        "confidence_level", result.getOrDefault("confidence_level", "REFUSED")
                ));
            }

            // ✅ Sauvegarder score en DB
            user u = service.getById(userId);
            Double score = result.get("score") != null
                    ? Double.valueOf(result.get("score").toString()) : 0.0;
            String level = result.getOrDefault("confidence_level", "LOW").toString();

            u.setFaceConfidence(score.floatValue());
            u.setFaceConfidenceLevel(level);
            service.update(userId, u);

            // ✅ Générer JWT
            String token = jwtService.generateToken(u.getEmail(), u.getRole().name());
            u.setPassword(null);

            return ResponseEntity.ok(Map.of(
                    "token",            token,
                    "user",             u,
                    "role",             u.getRole().name(),
                    "score",            score,
                    "confidence_level", level
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error",   "server_error",
                    "message", e.getMessage()
            ));
        }
    }

    /* ══════════════════════════════════════════
       VERIFY OTP — LOGIN
       ══════════════════════════════════════════ */

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp   = body.get("otp");

        if (email == null || otp == null) {
            return ResponseEntity.badRequest().body("Email and OTP are required");
        }

        if (!twoFaService.verifyOtpForLogin(email, otp)) {
            return ResponseEntity.status(400).body(Map.of(
                    "error", "invalid_otp",
                    "message", "Invalid or expired OTP"
            ));
        }

        user u = service.findByEmail(email);
        String token = jwtService.generateToken(email, u.getRole().name());
        u.setPassword(null);

        return ResponseEntity.ok(new AuthResponse(token, u, u.getRole().name()));
    }

    /* ══════════════════════════════════════════
       RESEND OTP
       ══════════════════════════════════════════ */

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null) return ResponseEntity.badRequest().body("Email is required");

        boolean sent = twoFaService.sendOtp(email);
        if (!sent) return ResponseEntity.status(404).body("User not found");

        return ResponseEntity.ok(Map.of("message", "OTP resent successfully"));
    }

    /* ══════════════════════════════════════════
       LOGOUT
       ══════════════════════════════════════════ */

    @PostMapping("/logout")
    public void logout() {
        // Stateless JWT: logout is client-side
    }

    /* ══════════════════════════════════════════
       ME — pour OAuth2 callback
       ══════════════════════════════════════════ */

    @GetMapping("/me")
    public ResponseEntity<?> getMe(
            @RequestHeader(name = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }

        String token = authorization.substring(7);
        if (!jwtService.isValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        String email = jwtService.extractEmail(token);
        user u = service.findByEmail(email);

        if (u == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        u.setPassword(null);
        return ResponseEntity.ok(u);
    }

    @GetMapping("/session")
    public user session(@RequestHeader(name = "Authorization", required = false) String authorization) {
        String token = extractBearerTokenOrThrow(authorization);
        if (!jwtService.isValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }

        String email = jwtService.extractEmail(token);
        user u = service.findByEmail(email);
        if (u == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        u.setPassword(null);
        return u;
    }
/* ══════════════════════════════════════════
   UPLOAD PHOTO → CLOUDINARY
   ══════════════════════════════════════════ */

    @PostMapping("/{id}/uploadPhoto")
    public ResponseEntity<String> uploadPhoto(@PathVariable Long id,
                                              @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return new ResponseEntity<>("No file selected", HttpStatus.BAD_REQUEST);
        }
        try {
            // ✅ Upload vers Cloudinary (plus de stockage local)
            String imageUrl = cloudinaryService.uploadImage(file);

            user existingUser = service.getById(id);
            if (existingUser == null) {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }

            existingUser.setPhotoUrl(imageUrl);
            service.update(id, existingUser);

            return new ResponseEntity<>(imageUrl, HttpStatus.OK);

        } catch (IOException e) {
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    /* ══════════════════════════════════════════
       CRUD
       ══════════════════════════════════════════ */

    @GetMapping("/{id}")
    public user getById(@PathVariable Long id) {
        user u = service.getById(id);
        if (u != null) u.setPassword(null);
        return u;
    }

    @GetMapping
    public List<user> getAll() {
        List<user> users = service.getAll();
        users.forEach(x -> x.setPassword(null));
        return users;
    }

    @GetMapping("/by-email")
    public user getByEmail(@RequestParam String email) {
        user u = service.findByEmail(email);
        if (u != null) u.setPassword(null);
        return u;
    }

    @PutMapping("/{id}")
    public user update(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        user existing = service.getById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        if (req.email() != null && !req.email().equals(existing.getEmail())) {
            user emailOwner = service.findByEmail(req.email());
            if (emailOwner != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already used");
            }
            existing.setEmail(req.email());
        }

        if (req.fullName()  != null) existing.setFullName(req.fullName());
        if (req.photoUrl()  != null) existing.setPhotoUrl(req.photoUrl());
        if (req.role()      != null) existing.setRole(role.valueOf(req.role()));
        if (req.password()  != null && !req.password().isBlank())
            existing.setPassword(passwordEncoder.encode(req.password()));

        user updated = service.update(id, existing);
        updated.setPassword(null);
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PutMapping("/{id}/role")
    public user updateRole(@PathVariable Long id, @RequestParam String role) {
        user updated = service.updateRole(id, role);
        updated.setPassword(null);
        return updated;
    }

    /* ══════════════════════════════════════════
       PASSWORD RESET
       ══════════════════════════════════════════ */

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        user u = service.findByEmail(email);
        if (u == null) {
            return ResponseEntity.ok("If the email exists, a reset link was sent.");
        }

        String token = java.util.UUID.randomUUID().toString();
        u.setResetToken(token);
        u.setResetTokenExpiry(java.time.Instant.now().plusSeconds(900));
        service.update(u.getId(), u);
        emailService.sendResetEmail(email, token);

        return ResponseEntity.ok("If the email exists, a reset link was sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token,
                                                @RequestParam String newPassword) {
        user u = service.findByResetToken(token);
        if (u == null || u.getResetTokenExpiry() == null ||
                u.getResetTokenExpiry().isBefore(java.time.Instant.now())) {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }

        u.setPassword(passwordEncoder.encode(newPassword));
        u.setResetToken(null);
        u.setResetTokenExpiry(null);
        service.update(u.getId(), u);

        return ResponseEntity.ok("Password updated successfully");
    }

    /* ══════════════════════════════════════════
       BLOCK / UNBLOCK
       ══════════════════════════════════════════ */

    @PutMapping("/{id}/block")
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        user u = service.getById(id);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        u.setBlocked(true);
        user updated = service.update(id, u);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        user u = service.getById(id);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        u.setBlocked(false);
        user updated = service.update(id, u);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }


    @PostMapping("/register-face")
    public ResponseEntity<?> registerFaceOnly(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("userId") == null || body.get("embedding") == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "userId and embedding are required"
                ));
            }

            Long userId = Long.valueOf(body.get("userId").toString());
            List<Double> embedding = (List<Double>) body.get("embedding");

            if (embedding.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "embedding is empty"
                ));
            }

            boolean registered = faceAuthService.registerFace(userId, embedding);

            if (!registered) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                        "success", false,
                        "message", "Face registration failed"
                ));
            }

            // ✅ Vérifier immédiatement pour obtenir le score initial
            Map<String, Object> verifyResult = faceAuthService.verifyFace(userId, embedding);
            Double score = verifyResult.get("score") != null
                    ? Double.valueOf(verifyResult.get("score").toString()) : 100.0;
            String level = verifyResult.getOrDefault("confidence_level", "HIGH").toString();

            // ✅ Sauvegarder le score
            user u = service.getById(userId);
            u.setFaceConfidence(score.floatValue());
            u.setFaceConfidenceLevel(level);
            service.update(userId, u);

            // ✅ Générer JWT
            String token = jwtService.generateToken(u.getEmail(), u.getRole().name());
            u.setPassword(null);

            return ResponseEntity.ok(new AuthResponse(token, u, u.getRole().name()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "server_error",
                    "message", e.getMessage()
            ));
        }
    }
    /* ══════════════════════════════════════════
       HELPERS
       ══════════════════════════════════════════ */

    private String extractBearerTokenOrThrow(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        }
        return authorization.substring(7);
    }
    @PutMapping("/{id}/emotion")
    public ResponseEntity<?> updateEmotion(
            @PathVariable Long id,
            @RequestParam String emotion) {

        user u = service.getById(id);
        if (u == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        u.setLastEmotion(emotion);
        user updated = service.update(id, u);
        updated.setPassword(null);
        return ResponseEntity.ok(updated);
    }





}