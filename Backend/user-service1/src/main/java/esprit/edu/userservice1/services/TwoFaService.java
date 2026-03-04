package esprit.edu.userservice1.services;

import esprit.edu.userservice1.entities.user;
import esprit.edu.userservice1.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TwoFaService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /* ── Générer OTP 6 chiffres ───────────────────── */
    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    /* ── Envoyer OTP ──────────────────────────────── */
    public boolean sendOtp(String email) {
        Optional<user> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return false;

        user u = opt.get();
        String otp = generateOtp();

        u.setOtpCode(otp);
        u.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(u);

        emailService.sendOtpEmail(email, otp, u.getFullName());
        return true;
    }

    /* ── Vérifier OTP (Register) ──────────────────── */
    public boolean verifyOtpForRegister(String email, String otpInput) {
        Optional<user> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return false;

        user u = opt.get();

        if (u.getOtpExpiry() == null || LocalDateTime.now().isAfter(u.getOtpExpiry()))
            return false;

        if (!otpInput.equals(u.getOtpCode()))
            return false;

        // ✅ Activer le compte
        u.setOtpCode(null);
        u.setOtpExpiry(null);
        u.setEmailVerified(true);
        u.setLast2faVerifiedAt(LocalDateTime.now());
        userRepository.save(u);
        return true;
    }

    /* ── Vérifier OTP (Login) ─────────────────────── */
    public boolean verifyOtpForLogin(String email, String otpInput) {
        Optional<user> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) return false;

        user u = opt.get();

        if (u.getOtpExpiry() == null || LocalDateTime.now().isAfter(u.getOtpExpiry()))
            return false;

        if (!otpInput.equals(u.getOtpCode()))
            return false;

        // ✅ Mettre à jour la date de dernière vérification
        u.setOtpCode(null);
        u.setOtpExpiry(null);
        u.setLast2faVerifiedAt(LocalDateTime.now()); // ← reset les 3 jours
        userRepository.save(u);
        return true;
    }


}