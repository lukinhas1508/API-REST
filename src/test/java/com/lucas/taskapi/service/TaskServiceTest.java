package com.lucas.taskapi.service;

import com.lucas.taskapi.dto.TaskRequest;
import com.lucas.taskapi.dto.TaskResponse;
import com.lucas.taskapi.dto.TaskStatusRequest;
import com.lucas.taskapi.entity.Task;
import com.lucas.taskapi.entity.TaskPriority;
import com.lucas.taskapi.entity.TaskStatus;
import com.lucas.taskapi.entity.User;
import com.lucas.taskapi.exception.BusinessException;
import com.lucas.taskapi.exception.ResourceNotFoundException;
import com.lucas.taskapi.mapper.TaskMapper;
import com.lucas.taskapi.repository.TaskRepository;
import com.lucas.taskapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long TASK_ID = 10L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TaskMapper taskMapper = new TaskMapper();

    @InjectMocks
    private TaskService taskService;

    private final User owner = new User("Lucas", "lucas@email.com", "senha-criptografada");

    private Task newTask() {
        return new Task("Estudar Spring", "Ler a documentação", TaskPriority.HIGH, null, owner);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve criar tarefa com status PENDING vinculada ao usuário")
        void shouldCreateWithPendingStatus() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.create(USER_ID,
                    new TaskRequest("Estudar Spring", "Ler a documentação", TaskPriority.HIGH, null));

            assertThat(response.title()).isEqualTo("Estudar Spring");
            assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
            assertThat(response.priority()).isEqualTo(TaskPriority.HIGH);
        }

        @Test
        @DisplayName("deve usar prioridade MEDIUM quando não informada")
        void shouldUseMediumPriorityByDefault() {
            when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
            when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.create(USER_ID, new TaskRequest("Estudar Spring", null, null, null));

            assertThat(response.priority()).isEqualTo(TaskPriority.MEDIUM);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar a tarefa do usuário")
        void shouldReturnTask() {
            when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID)).thenReturn(Optional.of(newTask()));

            TaskResponse response = taskService.findById(USER_ID, TASK_ID);

            assertThat(response.title()).isEqualTo("Estudar Spring");
        }

        @Test
        @DisplayName("deve lançar ResourceNotFoundException quando a tarefa não existir ou for de outro usuário")
        void shouldThrowWhenNotFound() {
            when(taskRepository.findByIdAndOwnerId(99L, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.findById(USER_ID, 99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve buscar apenas tarefas do usuário com os filtros informados")
        void shouldPassFiltersToRepository() {
            Pageable pageable = PageRequest.of(0, 10);
            when(taskRepository.search(USER_ID, TaskStatus.PENDING, TaskPriority.HIGH, pageable))
                    .thenReturn(new PageImpl<>(List.of(newTask()), pageable, 1));

            var response = taskService.findAll(USER_ID, TaskStatus.PENDING, TaskPriority.HIGH, pageable);

            assertThat(response.content()).hasSize(1);
            assertThat(response.totalElements()).isEqualTo(1);
            verify(taskRepository).search(USER_ID, TaskStatus.PENDING, TaskPriority.HIGH, pageable);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve atualizar os dados da tarefa")
        void shouldUpdateTask() {
            when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID)).thenReturn(Optional.of(newTask()));
            when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.update(USER_ID, TASK_ID,
                    new TaskRequest("Novo título", "Nova descrição", TaskPriority.LOW, null));

            assertThat(response.title()).isEqualTo("Novo título");
            assertThat(response.description()).isEqualTo("Nova descrição");
            assertThat(response.priority()).isEqualTo(TaskPriority.LOW);
        }

        @Test
        @DisplayName("não deve permitir editar tarefa concluída")
        void shouldNotUpdateDoneTask() {
            Task task = newTask();
            task.changeStatus(TaskStatus.DONE);
            when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.update(USER_ID, TASK_ID,
                    new TaskRequest("Novo título", null, null, null)))
                    .isInstanceOf(BusinessException.class);

            verify(taskRepository, never()).saveAndFlush(any(Task.class));
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("deve preencher completedAt ao concluir a tarefa")
        void shouldSetCompletedAtWhenDone() {
            when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID)).thenReturn(Optional.of(newTask()));
            when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.updateStatus(USER_ID, TASK_ID, new TaskStatusRequest(TaskStatus.DONE));

            assertThat(response.status()).isEqualTo(TaskStatus.DONE);
            assertThat(response.completedAt()).isNotNull();
        }

        @Test
        @DisplayName("deve limpar completedAt ao reabrir a tarefa")
        void shouldClearCompletedAtWhenReopened() {
            Task task = newTask();
            task.changeStatus(TaskStatus.DONE);
            when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));
            when(taskRepository.saveAndFlush(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

            TaskResponse response = taskService.updateStatus(USER_ID, TASK_ID,
                    new TaskStatusRequest(TaskStatus.IN_PROGRESS));

            assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(response.completedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deve remover tarefa do usuário")
        void shouldDeleteTask() {
            Task task = newTask();
            when(taskRepository.findByIdAndOwnerId(TASK_ID, USER_ID)).thenReturn(Optional.of(task));

            taskService.delete(USER_ID, TASK_ID);

            verify(taskRepository).delete(task);
        }

        @Test
        @DisplayName("não deve remover quando a tarefa não existir")
        void shouldNotDeleteWhenNotFound() {
            when(taskRepository.findByIdAndOwnerId(99L, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.delete(USER_ID, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(taskRepository, never()).delete(any(Task.class));
        }
    }
}
