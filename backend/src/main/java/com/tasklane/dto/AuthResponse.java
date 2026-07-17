package com.tasklane.dto;

public record AuthResponse(String token, UserView user) {
}
