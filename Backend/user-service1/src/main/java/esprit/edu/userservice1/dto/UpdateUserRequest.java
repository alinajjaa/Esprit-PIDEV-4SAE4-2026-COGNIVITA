package esprit.edu.userservice1.dto;

public record UpdateUserRequest(
        String fullName,
        String email,
        String photoUrl,
        String role,     // USER / ADMIN
        String password  // facultatif
) {}