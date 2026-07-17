package com.tasklane.service;

import com.tasklane.model.Task;
import com.tasklane.model.TaskPriority;
import com.tasklane.model.TaskStatus;
import com.tasklane.model.UserAccount;
import com.tasklane.model.UserRole;
import com.tasklane.repository.TaskRepository;
import com.tasklane.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public List<Task> findAll(UserAccount actor, String search, TaskStatus status, TaskPriority priority) {
        List<Task> visibleTasks = actor.getRole() == UserRole.ADMIN
                ? taskRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt"))
                : taskRepository.findAllByOwnerId(actor.getId());
        return visibleTasks.stream()
                .filter(task -> !StringUtils.hasText(search) || matchesSearch(task, search))
                .filter(task -> status == null || task.getStatus() == status)
                .filter(task -> priority == null || task.getPriority() == priority)
                .toList();
    }

    public Task findById(UserAccount actor, String id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
        if (actor.getRole() != UserRole.ADMIN && !actor.getId().equals(task.getOwnerId())) {
            throw new TaskNotFoundException(id);
        }
        return task;
    }

    public Task create(UserAccount actor, Task task) {
        task.setId(null);
        assignOwner(actor, task, task.getOwnerId());
        return taskRepository.save(task);
    }

    public Task update(UserAccount actor, String id, Task input) {
        Task task = findById(actor, id);
        task.setTitle(input.getTitle());
        task.setDescription(input.getDescription());
        task.setStatus(input.getStatus());
        task.setPriority(input.getPriority());
        task.setProject(input.getProject());
        task.setDueDate(input.getDueDate());
        task.setTags(input.getTags());
        task.setStoryPoints(input.getStoryPoints());
        assignOwner(actor, task, input.getOwnerId());
        return taskRepository.save(task);
    }

    public void delete(UserAccount actor, String id) {
        Task task = findById(actor, id);
        taskRepository.delete(task);
    }

    private boolean matchesSearch(Task task, String search) {
        String query = search.toLowerCase();
        return contains(task.getTitle(), query)
                || contains(task.getDescription(), query)
                || contains(task.getAssignee(), query)
                || contains(task.getProject(), query)
                || task.getTags() != null && task.getTags().stream().anyMatch(tag -> contains(tag, query));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private void assignOwner(UserAccount actor, Task task, String requestedOwnerId) {
        if (actor.getRole() == UserRole.USER) {
            task.setOwnerId(actor.getId());
            task.setAssignee(actor.getFullName());
            return;
        }

        String ownerId = StringUtils.hasText(requestedOwnerId) ? requestedOwnerId
                : StringUtils.hasText(task.getOwnerId()) ? task.getOwnerId() : actor.getId();
        UserAccount owner = userRepository.findById(ownerId)
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Choose an active assignee"));
        task.setOwnerId(owner.getId());
        task.setAssignee(owner.getFullName());
    }
}
