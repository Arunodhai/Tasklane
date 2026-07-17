package com.tasklane.config;

import com.tasklane.model.Task;
import com.tasklane.model.TaskPriority;
import com.tasklane.model.TaskStatus;
import com.tasklane.model.UserAccount;
import com.tasklane.model.UserRole;
import com.tasklane.repository.TaskRepository;
import com.tasklane.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "tasklane.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer {

    @Bean
    CommandLineRunner loadDemoData(
            TaskRepository taskRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${tasklane.demo-data.admin-email}") String adminEmail,
            @Value("${tasklane.demo-data.admin-password}") String adminPassword,
            @Value("${tasklane.demo-data.user-email}") String userEmail,
            @Value("${tasklane.demo-data.user-password}") String userPassword) {
        return args -> {
            ensureAccount(userRepository, passwordEncoder, "Tasklane Admin", adminEmail,
                    adminPassword, UserRole.ADMIN);
            UserAccount maya = ensureAccount(userRepository, passwordEncoder, "Maya Chen", userEmail,
                    userPassword, UserRole.USER);
            UserAccount noah = ensureAccount(userRepository, passwordEncoder, "Noah Williams",
                    "noah@tasklane.local", userPassword, UserRole.USER);
            UserAccount ava = ensureAccount(userRepository, passwordEncoder, "Ava Patel",
                    "ava@tasklane.local", userPassword, UserRole.USER);
            UserAccount ethan = ensureAccount(userRepository, passwordEncoder, "Ethan Brown",
                    "ethan@tasklane.local", userPassword, UserRole.USER);

            Map<String, DemoAssignment> assignments = Map.of(
                    "Finalize onboarding flow", new DemoAssignment(maya, 5, List.of("Launch", "UX")),
                    "Prepare customer interview notes", new DemoAssignment(noah, 3, List.of("Research")),
                    "Resolve mobile navigation issues", new DemoAssignment(ava, 8, List.of("Frontend", "Mobile")),
                    "Publish release checklist", new DemoAssignment(ethan, 2, List.of("Release")),
                    "Audit analytics events", new DemoAssignment(maya, 3, List.of("Analytics")),
                    "Update API error responses", new DemoAssignment(noah, 5, List.of("Backend", "API"))
            );

            if (taskRepository.count() == 0) {
                taskRepository.saveAll(List.of(
                        task("Finalize onboarding flow", "Review copy and edge cases before release.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "Product launch", 2, assignments),
                        task("Prepare customer interview notes", "Summarize recurring themes from the last six calls.", TaskStatus.TODO, TaskPriority.MEDIUM, "Research", 5, assignments),
                        task("Resolve mobile navigation issues", "Fix clipping and focus behavior on smaller screens.", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, "Web application", 1, assignments),
                        task("Publish release checklist", "Document owners and verification steps for launch day.", TaskStatus.DONE, TaskPriority.MEDIUM, "Product launch", -1, assignments),
                        task("Audit analytics events", "Confirm event names and required properties.", TaskStatus.TODO, TaskPriority.LOW, "Analytics", 8, assignments),
                        task("Update API error responses", "Align validation messages with the client application.", TaskStatus.DONE, TaskPriority.HIGH, "Platform", -3, assignments)
                ));
            } else {
                List<Task> migratedTasks = taskRepository.findAll().stream()
                        .filter(task -> assignments.containsKey(task.getTitle())
                                && (task.getStoryPoints() == null || "Demo User".equals(task.getAssignee())))
                        .toList();
                migratedTasks.forEach(task -> applyAssignment(task, assignments.get(task.getTitle())));
                taskRepository.saveAll(migratedTasks);
            }
        };
    }

    private Task task(String title, String description, TaskStatus status, TaskPriority priority,
                      String project, int dueInDays, Map<String, DemoAssignment> assignments) {
        Task task = Task.builder()
                .title(title)
                .description(description)
                .status(status)
                .priority(priority)
                .project(project)
                .dueDate(LocalDate.now().plusDays(dueInDays))
                .build();
        applyAssignment(task, assignments.get(title));
        return task;
    }

    private void applyAssignment(Task task, DemoAssignment assignment) {
        task.setOwnerId(assignment.user().getId());
        task.setAssignee(assignment.user().getFullName());
        task.setStoryPoints(assignment.storyPoints());
        task.setTags(assignment.tags());
    }

    private UserAccount ensureAccount(UserRepository repository, PasswordEncoder passwordEncoder,
                                      String fullName, String email, String password, UserRole role) {
        UserAccount account = repository.findByEmailIgnoreCase(email)
                .orElseGet(() -> repository.save(account(
                        fullName, email, password, role, passwordEncoder)));
        if (!fullName.equals(account.getFullName())) {
            account.setFullName(fullName);
            account = repository.save(account);
        }
        return account;
    }

    private UserAccount account(String fullName, String email, String password, UserRole role,
                                PasswordEncoder passwordEncoder) {
        return UserAccount.builder()
                .fullName(fullName)
                .email(email.toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build();
    }

    private record DemoAssignment(UserAccount user, int storyPoints, List<String> tags) {}
}
