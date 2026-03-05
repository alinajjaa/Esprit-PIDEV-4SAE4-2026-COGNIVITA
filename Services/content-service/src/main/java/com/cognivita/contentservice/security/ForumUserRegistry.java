package com.cognivita.contentservice.security;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ForumUserRegistry {

    private static final Set<String> USERS = Set.of(
            "alice",
            "bob",
            "charlie",
            "diana",
            "moderator"
    );

    public String requireValidUser(String username, String fieldName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        String normalized = username.trim();
        if (!USERS.contains(normalized)) {
            throw new IllegalArgumentException(
                    fieldName + " must be one of: " + String.join(", ", USERS)
            );
        }
        return normalized;
    }
}

