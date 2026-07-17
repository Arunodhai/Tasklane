package com.tasklane.dto;

import com.tasklane.model.UserAccount;
import com.tasklane.model.UserRole;

import java.time.Instant;

public record UserView(
        String id,
        String fullName,
        String email,
        UserRole role,
        boolean enabled,
        Instant createdAt
) {
    public static UserView from(UserAccount user) {
        return new UserView(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
