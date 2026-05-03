package esprit.edu.userservice1.security;

import esprit.edu.userservice1.entities.role;
import esprit.edu.userservice1.entities.user;
import esprit.edu.userservice1.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();

        String provider = oauthToken.getAuthorizedClientRegistrationId();

        String email   = null;
        String name    = null;
        String picture = null;

        if ("google".equals(provider)) {
            // ── Google : email toujours disponible ─────────
            email   = oAuth2User.getAttribute("email");
            name    = oAuth2User.getAttribute("name");
            picture = oAuth2User.getAttribute("picture");

        } else if ("facebook".equals(provider)) {
            // ── Facebook : email pas toujours disponible ───
            String fbId      = oAuth2User.getAttribute("id");
            String firstName = oAuth2User.getAttribute("first_name");
            String lastName  = oAuth2User.getAttribute("last_name");

            // Essayer d'avoir l'email réel
            String fbEmail = oAuth2User.getAttribute("email");

            // ✅ Si email non disponible → email fictif basé sur fbId
            email = (fbEmail != null && !fbEmail.isEmpty())
                    ? fbEmail
                    : fbId + "@facebook.com";

            name = (firstName != null ? firstName : "")
                    + (lastName  != null ? " " + lastName : "");
            name = name.trim();

            if (name.isEmpty()) {
                name = oAuth2User.getAttribute("name");
            }

            // ✅ Extraire photo Facebook
            try {
                Map<String, Object> pictureObj = oAuth2User.getAttribute("picture");
                if (pictureObj != null) {
                    Map<String, Object> data = (Map<String, Object>) pictureObj.get("data");
                    if (data != null) {
                        picture = (String) data.get("url");
                    }
                }
            } catch (Exception e) {
                picture = null;
            }
        }

        // ── Email toujours obligatoire ─────────────────────
        if (email == null) {
            System.err.println("OAuth2 Error: email is null for provider " + provider);
            getRedirectStrategy().sendRedirect(request, response,
                    "http://localhost:4200/login?error=email_not_found");
            return;
        }

        System.out.println("OAuth2 Success — provider: " + provider + " | email: " + email);

        final String finalEmail   = email;
        final String finalName    = name;
        final String finalPicture = picture;

        // ── Trouver ou créer l'utilisateur ────────────────
        Optional<user> existing = userRepository.findByEmail(email);

        user u;
        if (existing.isPresent()) {
            u = existing.get();
            // Mettre à jour la photo si elle a changé
            if (finalPicture != null && !finalPicture.equals(u.getPhotoUrl())) {
                u.setPhotoUrl(finalPicture);
                userRepository.save(u);
            }
        } else {
            u = new user();
            u.setEmail(finalEmail);
            u.setFullName(finalName != null && !finalName.isEmpty() ? finalName : "User");
            u.setPassword(UUID.randomUUID().toString());
            u.setRole(role.USER);
            u.setPhotoUrl(finalPicture);
            u = userRepository.save(u);
            System.out.println("✅ New user created: " + finalEmail);
        }

        // ── Générer JWT ────────────────────────────────────
        String roleStr = u.getRole() != null ? u.getRole().name() : "USER";
        String token   = jwtService.generateToken(email, roleStr);

        System.out.println("✅ JWT generated for: " + email + " | role: " + roleStr);

        // ── Rediriger vers Angular ─────────────────────────
        String redirectUrl = "http://localhost:4200/oauth2/callback"
                + "?token=" + token
                + "&role="  + roleStr;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}