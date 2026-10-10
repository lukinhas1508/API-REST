package com.lucas.taskapi.service;

import com.lucas.taskapi.dto.PageResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Todas as operações recebem o id do usuário autenticado,
 * garantindo que cada usuário acesse apenas as próprias tarefas.
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findAll(Long userId, TaskStatus status, TaskPriority priority, Pageable pageable) {
        Page<TaskResponse> page = taskRepository.search(userId, status, priority, pageable)
                .map(taskMapper::toResponse);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long userId, Long id) {
        return taskMapper.toResponse(getTaskOrThrow(userId, id));
    }

    @Transactional
    public TaskResponse create(Long userId, TaskRequest request) {
        User owner = userRepository.getReferenceById(userId);
        Task saved = taskRepository.save(taskMapper.toEntity(request, owner));
        log.info("Tarefa {} criada pelo usuário {}", saved.getId(), userId);
        return taskMapper.toResponse(saved);
    }

    @Transactional
    public TaskResponse update(Long userId, Long id, TaskRequest request) {
        Task task = getTaskOrThrow(userId, id);

        if (task.isDone()) {
            throw new BusinessException("Não é possível editar uma tarefa concluída. Reabra a tarefa antes de editar.");
        }

        taskMapper.updateEntity(task, request);
        // flush para o @PreUpdate rodar e o updatedAt voltar atualizado na resposta
        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long userId, Long id, TaskStatusRequest request) {
        Task task = getTaskOrThrow(userId, id);
        task.changeStatus(request.status());
        log.info("Status da tarefa {} alterado para {}", id, request.status());
        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Task task = getTaskOrThrow(userId, id);
        taskRepository.delete(task);
        log.info("Tarefa {} removida pelo usuário {}", id, userId);
    }

    /**
     * Retorna 404 tanto para tarefa inexistente quanto para tarefa de outro usuário,
     * para não revelar a existência de recursos que não pertencem a quem pediu.
     */
    private Task getTaskOrThrow(Long userId, Long id) {
        return taskRepository.findByIdAndOwnerId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa com id " + id + " não encontrada"));
    }
}
