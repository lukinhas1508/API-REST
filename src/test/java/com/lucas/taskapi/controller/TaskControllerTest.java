package com.lucas.taskapi.controller;

import com.lucas.taskapi.config.SecurityConfig;
import com.lucas.taskapi.dto.TaskRequest;
import com.lucas.taskapi.dto.TaskResponse;
import com.lucas.taskapi.entity.TaskPriority;
import com.lucas.taskapi.entity.TaskStatus;
import com.lucas.taskapi.exception.BusinessException;
import com.lucas.taskapi.exception.ResourceNotFoundException;
import com.lucas.taskapi.service.TaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class TaskControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    /** Simula uma requisição autenticada com um JWT cujo subject é o id do usuário. */
    private RequestPostProcessor authenticatedUser() {
        return jwt().jwt(token -> token.subject(USER_ID.toString()));
    }

    private TaskResponse sampleResponse() {
        LocalDateTime now = LocalDateTime.now();
        return new TaskResponse(1L, "Estudar Spring", "Ler a documentação",
                TaskStatus.PENDING, TaskPriority.HIGH, null, null, now, now);
    }

    @Test
    @DisplayName("deve retornar 401 quando não houver token")
    void shouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(taskService);
    }

    @Test
    @DisplayName("POST /tasks deve retornar 201 com header Location")
    void shouldCreateTask() throws Exception {
        when(taskService.create(eq(USER_ID), any(TaskRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/tasks")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Estudar Spring", "description": "Ler a documentação", "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/tasks/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /tasks deve retornar 400 quando o título estiver vazio")
    void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.errors.title").value("O título é obrigatório"));
    }

    @Test
    @DisplayName("POST /tasks deve retornar 400 quando a prioridade for inválida")
    void shouldReturnBadRequestWhenPriorityIsInvalid() throws Exception {
        mockMvc.perform(post("/tasks")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Estudar Spring", "priority": "URGENTE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    @DisplayName("GET /tasks/{id} deve retornar 404 quando a tarefa não existir")
    void shouldReturnNotFound() throws Exception {
        when(taskService.findById(USER_ID, 99L))
                .thenThrow(new ResourceNotFoundException("Tarefa com id 99 não encontrada"));

        mockMvc.perform(get("/tasks/99").with(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Tarefa com id 99 não encontrada"))
                .andExpect(jsonPath("$.instance").value("/tasks/99"));
    }

    @Test
    @DisplayName("PUT /tasks/{id} deve retornar 422 ao editar tarefa concluída")
    void shouldReturnUnprocessableWhenEditingDoneTask() throws Exception {
        when(taskService.update(eq(USER_ID), eq(1L), any(TaskRequest.class)))
                .thenThrow(new BusinessException("Não é possível editar uma tarefa concluída"));

        mockMvc.perform(put("/tasks/1")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Novo título"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    @DisplayName("PATCH /tasks/{id}/status deve retornar 400 sem status")
    void shouldReturnBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(patch("/tasks/1/status")
                        .with(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("O status é obrigatório"));
    }

    @Test
    @DisplayName("DELETE /tasks/{id} deve retornar 204")
    void shouldDeleteTask() throws Exception {
        mockMvc.perform(delete("/tasks/1").with(authenticatedUser()))
                .andExpect(status().isNoContent());

        verify(taskService).delete(USER_ID, 1L);
    }
}
