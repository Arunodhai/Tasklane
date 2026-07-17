package com.tasklane.repository;

import com.tasklane.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TaskRepository extends MongoRepository<Task, String> {
    List<Task> findAllByOwnerId(String ownerId);
}
