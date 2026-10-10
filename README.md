# Task API

API REST para gerenciamento de tarefas, desenvolvida com Java 21 e Spring Boot 4.

O objetivo do projeto é aplicar boas práticas no desenvolvimento de APIs: arquitetura em camadas, regras de negócio isoladas no domínio, versionamento do banco com migrations, respostas de erro padronizadas e testes em diferentes níveis.

## Tecnologias

- **Java 21**
- **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Actuator)
- **PostgreSQL 17**
- **Flyway** para migrations
- **springdoc-openapi** para documentação (Swagger UI)
- **JUnit 5, Mockito, MockMvc e Testcontainers** para testes
- **Docker e Docker Compose**
- **GitHub Actions** para CI

## Funcionalidades

- CRUD completo de tarefas
- Alteração de status (`PENDING`, `IN_PROGRESS`, `DONE`) com registro da data de conclusão
- Regra de negócio: tarefas concluídas não podem ser editadas
- Listagem paginada com filtros por status e prioridade
- Validação dos dados de entrada com mensagens em português
- Erros no padrão **ProblemDetail (RFC 9457)**
- Health check em `/actuator/health`

## Arquitetura

```
src/main/java/com/lucas/taskapi
├── config       # configurações (OpenAPI)
├── controller   # camada HTTP: rotas, status e validação de entrada
├── service      # regras de negócio e transações
├── repository   # acesso a dados com Spring Data JPA
├── entity       # entidades JPA e enums do domínio
├── dto          # contratos de entrada e saída da API
├── mapper       # conversão entre entidades e DTOs
└── exception    # exceções de domínio e handler global
```

Algumas decisões:

- **DTOs separados das entidades**, para que mudanças no banco não quebrem o contrato da API.
- **Regras de estado dentro da entidade** (`changeStatus`), evitando que o status seja alterado sem atualizar a data de conclusão.
- **`ddl-auto: validate` + Flyway**: o schema é controlado só pelas migrations, e o Hibernate apenas confere se o mapeamento bate com o banco.
- **`open-in-view: false`**, para não manter conexão com o banco aberta durante a renderização da resposta.
- **Resposta paginada própria (`PageResponse`)**, para não expor a estrutura interna do `Page` do Spring.

## Como rodar

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker

### Opção 1: tudo com Docker

```bash
docker compose up --build
```

### Opção 2: banco no Docker e aplicação local (desenvolvimento)

```bash
docker compose up -d db
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080`.

As variáveis de ambiente do banco estão em `.env.example`. Os valores padrão já funcionam com o Docker Compose.

## Documentação

Com a aplicação rodando, acesse o Swagger UI:

```
http://localhost:8080/swagger-ui.html
```

## Testes

```bash
mvn test
```

| Tipo        | Classe                | O que cobre                                            |
|-------------|-----------------------|--------------------------------------------------------|
| Unitário    | `TaskServiceTest`     | Regras de negócio, com o repository mockado            |
| Web (slice) | `TaskControllerTest`  | Rotas, status HTTP, validação e formato dos erros      |
| Integração  | `TaskIntegrationTest` | Fluxo completo com PostgreSQL real via Testcontainers  |

> O teste de integração precisa do Docker rodando.

## Endpoints

| Método | Rota                 | Descrição                    | Sucesso |
|--------|----------------------|------------------------------|---------|
| GET    | `/tasks`             | Lista tarefas (paginado)     | 200     |
| GET    | `/tasks/{id}`        | Busca tarefa por id          | 200     |
| POST   | `/tasks`             | Cria tarefa                  | 201     |
| PUT    | `/tasks/{id}`        | Atualiza dados da tarefa     | 200     |
| PATCH  | `/tasks/{id}/status` | Altera o status              | 200     |
| DELETE | `/tasks/{id}`        | Remove tarefa                | 204     |

### Filtros e paginação

`GET /tasks` aceita:

- `status`: `PENDING`, `IN_PROGRESS` ou `DONE`
- `priority`: `LOW`, `MEDIUM` ou `HIGH`
- `page`: número da página, começando em 0
- `size`: itens por página (padrão 10, máximo 50)
- `sort`: campo e direção, por exemplo `dueDate,asc`

Exemplo: `GET /tasks?status=PENDING&priority=HIGH&sort=dueDate,asc`

## Exemplos

### Criar tarefa

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Estudar Spring Boot", "priority": "HIGH", "dueDate": "2026-12-01"}'
```

```json
{
  "id": 1,
  "title": "Estudar Spring Boot",
  "description": null,
  "status": "PENDING",
  "priority": "HIGH",
  "dueDate": "2026-12-01",
  "completedAt": null,
  "createdAt": "2026-10-10T12:00:00",
  "updatedAt": "2026-10-10T12:00:00"
}
```

### Concluir tarefa

```bash
curl -X PATCH http://localhost:8080/tasks/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}'
```

### Erro de validação (400)

```json
{
  "type": "about:blank",
  "title": "Erro de validação",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos",
  "instance": "/tasks",
  "errors": {
    "title": "O título é obrigatório"
  }
}
```

### Regra de negócio violada (422)

```json
{
  "type": "about:blank",
  "title": "Regra de negócio violada",
  "status": 422,
  "detail": "Não é possível editar uma tarefa concluída. Reabra a tarefa antes de editar.",
  "instance": "/tasks/1"
}
```

## Próximos passos

- [ ] Autenticação com Spring Security e JWT
- [ ] Tarefas vinculadas ao usuário autenticado
- [ ] Deploy em nuvem
