package com.lucas.taskapi.dto;

import com.lucas.taskapi.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(

        @NotNull(message = "O status é obrigatório")
        TaskStatus status
) {
}
