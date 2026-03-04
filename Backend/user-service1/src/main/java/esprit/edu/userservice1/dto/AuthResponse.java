package esprit.edu.userservice1.dto;

import esprit.edu.userservice1.entities.user;

public record AuthResponse(String token, user user, String role) {
    public AuthResponse(String token, user user) {
        this(token, user, user.getRole() != null ? user.getRole().name() : "USER");
    }
}