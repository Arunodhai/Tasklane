package com.tasklane.dto;

import com.tasklane.model.UserRole;

public record UpdateUserRequest(UserRole role, Boolean enabled) {
}
