package com.tasklane.controller;

import com.tasklane.model.Task;
import com.tasklane.model.TaskPriority;
import com.tasklane.model.TaskStatus;
import com.tasklane.service.TaskService;
import com.tasklane.service.CurrentUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService taskService;
    private final CurrentUserService currentUserService;

    public TaskController(TaskService taskService, CurrentUserService currentUserService) {
        this.taskService = taskService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<Task> findAll(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority) {
        return taskService.findAll(currentUserService.require(authentication), search, status, priority);
    }

    @GetMapping("/{id}")
    public Task findById(Authentication authentication, @PathVariable String id) {
        return taskService.findById(currentUserService.require(authentication), id);
    }

    @PostMapping
    public ResponseEntity<Task> create(Authentication authentication, @Valid @RequestBody Task task) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(currentUserService.require(authentication), task));
    }

    @PutMapping("/{id}")
    public Task update(Authentication authentication, @PathVariable String id, @Valid @RequestBody Task task) {
        return taskService.update(currentUserService.require(authentication), id, task);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication authentication, @PathVariable String id) {
        taskService.delete(currentUserService.require(authentication), id);
        return ResponseEntity.noContent().build();
    }
}
