package com.lucas.taskapi.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTest {

    private final User owner = new User("Lucas", "lucas@email.com", "hash");

    private Task newTask() {
        return new Task("Estudar Spring", null, null, null, owner);
    }

    @Test
    @DisplayName("nova tarefa deve começar PENDING com prioridade MEDIUM")
    void shouldStartPendingWithMediumPriority() {
        Task task = newTask();

        assertThat(task.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.getCompletedAt()).isNull();
        assertThat(task.isDone()).isFalse();
    }

    @Test
    @DisplayName("concluir a tarefa deve registrar a data de conclusão")
    void shouldSetCompletedAtWhenDone() {
        Task task = newTask();

        task.changeStatus(TaskStatus.DONE);

        assertThat(task.isDone()).isTrue();
        assertThat(task.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("reabrir a tarefa deve limpar a data de conclusão")
    void shouldClearCompletedAtWhenReopened() {
        Task task = newTask();
        task.changeStatus(TaskStatus.DONE);

        task.changeStatus(TaskStatus.IN_PROGRESS);

        assertThat(task.isDone()).isFalse();
        assertThat(task.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("repetir o mesmo status não deve alterar a data de conclusão")
    void shouldKeepCompletedAtWhenStatusDoesNotChange() {
        Task task = newTask();
        task.changeStatus(TaskStatus.DONE);
        LocalDateTime firstCompletion = task.getCompletedAt();

        task.changeStatus(TaskStatus.DONE);

        assertThat(task.getCompletedAt()).isEqualTo(firstCompletion);
    }

    @Test
    @DisplayName("atualizar sem prioridade deve manter a prioridade atual")
    void shouldKeepPriorityWhenUpdateHasNone() {
        Task task = new Task("Estudar Spring", null, TaskPriority.HIGH, null, owner);

        task.update("Novo título", "Nova descrição", null, null);

        assertThat(task.getTitle()).isEqualTo("Novo título");
        assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
    }
}
