package esprit.edu.userservice1.dto;

import java.util.List;

public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String photoUrl
) {}