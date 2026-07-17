package com.tasklane.service;

import com.tasklane.dto.UpdateUserRequest;
import com.tasklane.dto.UserView;
import com.tasklane.model.UserAccount;
import com.tasklane.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserView> findAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(UserView::from)
                .toList();
    }

    public UserView update(UserAccount actor, String id, UpdateUserRequest request) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User account was not found"));
        if (actor.getId().equals(user.getId())
                && (request.role() != null || Boolean.FALSE.equals(request.enabled()))) {
            throw new IllegalArgumentException("You cannot demote or disable your own account");
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        return UserView.from(userRepository.save(user));
    }
}
