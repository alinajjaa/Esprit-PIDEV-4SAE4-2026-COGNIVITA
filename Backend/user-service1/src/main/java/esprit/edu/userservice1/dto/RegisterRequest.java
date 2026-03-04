package esprit.edu.userservice1.dto;

public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String photoUrl
) {}
