package com.lucas.taskapi.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira com um PostgreSQL real em container,
 * validando migrations, mapeamento JPA e o fluxo HTTP de ponta a ponta.
 * Requer Docker rodando na máquina.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("deve executar o ciclo de vida completo de uma tarefa")
    void shouldRunFullTaskLifecycle() throws Exception {
        // cria
        String body = mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Teste de integração", "priority": "HIGH"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").value(notNullValue()))
                .andReturn().getResponse().getContentAsString();

        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        // busca
        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // conclui
        mockMvc.perform(patch("/tasks/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value(notNullValue()));

        // não pode editar tarefa concluída
        mockMvc.perform(put("/tasks/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Título alterado"}
                                """))
                .andExpect(status().is(422));

        // aparece no filtro por status
        mockMvc.perform(get("/tasks").param("status", "DONE").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        // remove
        mockMvc.perform(delete("/tasks/{id}", id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/{id}", id))
                .andExpect(status().isNotFound());
    }
}
