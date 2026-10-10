package com.lucas.taskapi.dto;

import com.lucas.taskapi.entity.TaskPriority;

import java.time.LocalDate;

public record TaskRequest(
        String title,
        String description,
        TaskPriority priority,
        LocalDate dueDate
) {
}
