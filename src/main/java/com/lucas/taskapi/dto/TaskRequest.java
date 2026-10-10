package com.lucas.taskapi.dto;

import com.lucas.taskapi.entity.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres")
        String title,

        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String description,

        TaskPriority priority,

        @FutureOrPresent(message = "A data de entrega não pode estar no passado")
        LocalDate dueDate
) {
}
