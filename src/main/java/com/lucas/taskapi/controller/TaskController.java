package com.lucas.taskapi.controller;

import com.lucas.taskapi.dto.PageResponse;
import com.lucas.taskapi.dto.TaskRequest;
import com.lucas.taskapi.dto.TaskResponse;
import com.lucas.taskapi.dto.TaskStatusRequest;
import com.lucas.taskapi.entity.TaskPriority;
import com.lucas.taskapi.entity.TaskStatus;
import com.lucas.taskapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Tag(name = "Tarefas", description = "Gerenciamento de tarefas")
@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Lista tarefas com paginação e filtros opcionais")
    @GetMapping
    public ResponseEntity<PageResponse<TaskResponse>> findAll(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(taskService.findAll(currentUserId(jwt), status, priority, pageable));
    }

    @Operation(summary = "Busca uma tarefa pelo id")
    @ApiResponse(responseCode = "200", description = "Tarefa encontrada")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> findById(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(taskService.findById(currentUserId(jwt), id));
    }

    @Operation(summary = "Cria uma nova tarefa")
    @ApiResponse(responseCode = "201", description = "Tarefa criada")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @RequestBody @Valid TaskRequest request) {
        TaskResponse created = taskService.create(currentUserId(jwt), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Atualiza os dados de uma tarefa")
    @ApiResponse(responseCode = "200", description = "Tarefa atualizada")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    @ApiResponse(responseCode = "422", description = "Tarefa concluída não pode ser editada")
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                               @PathVariable Long id,
                                               @RequestBody @Valid TaskRequest request) {
        return ResponseEntity.ok(taskService.update(currentUserId(jwt), id, request));
    }

    @Operation(summary = "Altera o status de uma tarefa")
    @ApiResponse(responseCode = "200", description = "Status alterado")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable Long id,
                                                     @RequestBody @Valid TaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(currentUserId(jwt), id, request));
    }

    @Operation(summary = "Remove uma tarefa")
    @ApiResponse(responseCode = "204", description = "Tarefa removida")
    @ApiResponse(responseCode = "404", description = "Tarefa não encontrada")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
                                       @PathVariable Long id) {
        taskService.delete(currentUserId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private Long currentUserId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
