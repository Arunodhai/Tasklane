package com.tasklane.controller;

import com.tasklane.dto.UpdateUserRequest;
import com.tasklane.dto.UserView;
import com.tasklane.service.AdminService;
import com.tasklane.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {
    private final AdminService adminService;
    private final CurrentUserService currentUserService;

    public AdminController(AdminService adminService, CurrentUserService currentUserService) {
        this.adminService = adminService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<UserView> findAll() {
        return adminService.findAll();
    }

    @PatchMapping("/{id}")
    public UserView update(Authentication authentication, @PathVariable String id,
                           @RequestBody UpdateUserRequest request) {
        return adminService.update(currentUserService.require(authentication), id, request);
    }
}
