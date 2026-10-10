package com.lucas.taskapi.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

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
 * validando migrations, segurança, mapeamento JPA e o fluxo HTTP de ponta a ponta.
 * Requer Docker rodando na máquina.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class TaskIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    /** Cadastra um usuário com e-mail único, faz login e devolve o header Authorization pronto. */
    private String registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@email.com";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Usuário de Teste", "email": "%s", "password": "senha12345"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "senha12345"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(body, "$.accessToken");
        return "Bearer " + token;
    }

    private long createTask(String auth, String json) throws Exception {
        String body = mockMvc.perform(post("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    @DisplayName("deve executar o ciclo de vida completo de uma tarefa")
    void shouldRunFullTaskLifecycle() throws Exception {
        String auth = registerAndLogin();

        long id = createTask(auth, """
                {"title": "Teste de integração", "priority": "HIGH"}
                """);

        mockMvc.perform(get("/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));

        mockMvc.perform(patch("/tasks/{id}/status", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DONE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").value(notNullValue()));

        mockMvc.perform(put("/tasks/{id}", id)
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Título alterado"}
                                """))
                .andExpect(status().is(422));

        mockMvc.perform(get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .param("status", "DONE")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(delete("/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/{id}", id).header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("usuário não deve acessar tarefas de outro usuário")
    void shouldIsolateTasksBetweenUsers() throws Exception {
        String alice = registerAndLogin();
        String bob = registerAndLogin();

        long aliceTaskId = createTask(alice, """
                {"title": "Tarefa da Alice"}
                """);

        mockMvc.perform(get("/tasks/{id}", aliceTaskId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/tasks/{id}", aliceTaskId).header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("deve recusar requisições sem token ou com token inválido")
    void shouldRejectMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/tasks").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("deve recusar login com senha errada e cadastro com e-mail repetido")
    void shouldHandleAuthErrors() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@email.com";
        String registerJson = """
                {"name": "Lucas", "email": "%s", "password": "senha12345"}
                """.formatted(email);

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "senha-errada"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }
}
