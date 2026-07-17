package com.tasklane.service;

import com.tasklane.model.UserAccount;
import com.tasklane.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserAccount require(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required");
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User account is unavailable"));
    }
}
