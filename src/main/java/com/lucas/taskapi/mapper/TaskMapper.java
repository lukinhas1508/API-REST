package com.lucas.taskapi.mapper;

import com.lucas.taskapi.dto.TaskRequest;
import com.lucas.taskapi.dto.TaskResponse;
import com.lucas.taskapi.entity.Task;
import com.lucas.taskapi.entity.User;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    public Task toEntity(TaskRequest request, User owner) {
        return new Task(
                request.title(),
                request.description(),
                request.priority(),
                request.dueDate(),
                owner
        );
    }

    public void updateEntity(Task task, TaskRequest request) {
        task.update(
                request.title(),
                request.description(),
                request.priority(),
                request.dueDate()
        );
    }

    public TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
