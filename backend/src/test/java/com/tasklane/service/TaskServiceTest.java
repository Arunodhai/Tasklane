package com.tasklane.service;

import com.tasklane.model.Task;
import com.tasklane.model.TaskPriority;
import com.tasklane.model.TaskStatus;
import com.tasklane.model.UserAccount;
import com.tasklane.model.UserRole;
import com.tasklane.repository.TaskRepository;
import com.tasklane.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    private TaskRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaskService service;

    @Test
    void findsTaskById() {
        Task task = Task.builder()
                .id("task-1")
                .title("Prepare demo")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.HIGH)
                .assignee("Arunodhai V")
                .project("Interview")
                .build();
        when(repository.findById("task-1")).thenReturn(Optional.of(task));

        assertEquals("Prepare demo", service.findById(admin(), "task-1").getTitle());
    }

    @Test
    void reportsMissingTask() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(TaskNotFoundException.class, () -> service.findById(admin(), "missing"));
    }

    @Test
    void adminCanAssignTaskToActiveUser() {
        UserAccount assignee = member("user-2", "Maya Chen");
        Task task = taskForCreate();
        task.setOwnerId(assignee.getId());
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = service.create(admin(), task);

        assertEquals("user-2", saved.getOwnerId());
        assertEquals("Maya Chen", saved.getAssignee());
    }

    @Test
    void memberAssignmentIsAlwaysSelf() {
        UserAccount member = member("user-1", "Noah Williams");
        Task task = taskForCreate();
        task.setOwnerId("another-user");
        when(repository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Task saved = service.create(member, task);

        assertEquals("user-1", saved.getOwnerId());
        assertEquals("Noah Williams", saved.getAssignee());
    }

    private Task taskForCreate() {
        return Task.builder()
                .title("Plan sprint")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.MEDIUM)
                .assignee("Untrusted name")
                .project("Delivery")
                .storyPoints(5)
                .build();
    }

    private UserAccount admin() {
        return UserAccount.builder()
                .id("admin-1")
                .email("admin@tasklane.local")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build();
    }

    private UserAccount member(String id, String name) {
        return UserAccount.builder()
                .id(id)
                .fullName(name)
                .email(name.toLowerCase().replace(' ', '.') + "@tasklane.local")
                .role(UserRole.USER)
                .enabled(true)
                .build();
    }
}
