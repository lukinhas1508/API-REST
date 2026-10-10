package com.lucas.taskapi.service;

import com.lucas.taskapi.dto.TaskRequest;
import com.lucas.taskapi.dto.TaskResponse;
import com.lucas.taskapi.dto.TaskStatusRequest;
import com.lucas.taskapi.entity.Task;
import com.lucas.taskapi.exception.BusinessException;
import com.lucas.taskapi.exception.ResourceNotFoundException;
import com.lucas.taskapi.mapper.TaskMapper;
import com.lucas.taskapi.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll()
                .stream()
                .map(taskMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findById(Long id) {
        return taskMapper.toResponse(getTaskOrThrow(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Task saved = taskRepository.save(taskMapper.toEntity(request));
        log.info("Tarefa criada com id {}", saved.getId());
        return taskMapper.toResponse(saved);
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest request) {
        Task task = getTaskOrThrow(id);

        if (task.isDone()) {
            throw new BusinessException("Não é possível editar uma tarefa concluída. Reabra a tarefa antes de editar.");
        }

        taskMapper.updateEntity(task, request);
        // flush para o @PreUpdate rodar e o updatedAt voltar atualizado na resposta
        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatusRequest request) {
        Task task = getTaskOrThrow(id);
        task.changeStatus(request.status());
        log.info("Status da tarefa {} alterado para {}", id, request.status());
        return taskMapper.toResponse(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void delete(Long id) {
        Task task = getTaskOrThrow(id);
        taskRepository.delete(task);
        log.info("Tarefa {} removida", id);
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa com id " + id + " não encontrada"));
    }
}
