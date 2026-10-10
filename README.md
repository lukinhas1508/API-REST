# Task API

![CI](https://github.com/lukinhas1508/API-REST/actions/workflows/ci.yml/badge.svg?branch=main)

API REST para gerenciamento de tarefas com autenticação JWT, desenvolvida com Java 21 e Spring Boot 4.

Cada usuário se cadastra, faz login e gerencia apenas as próprias tarefas. O projeto aplica boas práticas de desenvolvimento de APIs: arquitetura em camadas, regras de negócio no domínio, migrations versionadas, erros padronizados, credenciais fora do código e testes automatizados em vários níveis.

## Tecnologias

- **Java 21**
- **Spring Boot 4.1** (Web MVC, Data JPA, Validation, Security, Actuator)
- **Spring Security + OAuth2 Resource Server** para autenticação JWT (HS256)
- **PostgreSQL 17** e **Flyway**
- **springdoc-openapi** (Swagger UI)
- **JUnit 5, Mockito, MockMvc e Testcontainers**
- **Docker, Docker Compose e GitHub Actions**

## Funcionalidades

- Cadastro e login de usuários com senha criptografada (BCrypt)
- Autenticação stateless com token JWT
- CRUD de tarefas, isolado por usuário
- Alteração de status (`PENDING`, `IN_PROGRESS`, `DONE`) com registro da data de conclusão
- Regra de negócio: tarefas concluídas não podem ser editadas
- Listagem paginada com filtros por status e prioridade
- Erros no padrão **ProblemDetail (RFC 9457)**
- Health check em `/actuator/health`

## Arquitetura

```
src/main/java/com/lucas/taskapi
├── config       # segurança, JWT e OpenAPI
├── controller   # camada HTTP: rotas, status e validação de entrada
├── service      # regras de negócio, autenticação e transações
├── repository   # acesso a dados com Spring Data JPA
├── entity       # entidades JPA e enums do domínio
├── dto          # contratos de entrada e saída da API
├── mapper       # conversão entre entidades e DTOs
└── exception    # exceções de domínio e handler global
```

Algumas decisões:

- **JWT com o resource server nativo do Spring Security**, sem filtro de autenticação escrito à mão: o próprio framework valida assinatura, emissor e expiração do token.
- **O id do usuário vai no `subject` do token**, e todas as consultas de tarefas filtram pelo dono.
- **404 para tarefa de outro usuário**, em vez de 403, para não revelar que o recurso existe.
- **Mesma mensagem para e-mail inexistente e senha errada** no login, evitando descobrir quais e-mails estão cadastrados.
- **DTOs separados das entidades**, para que mudanças no banco não quebrem o contrato da API.
- **Regras de estado na entidade** (`changeStatus`), evitando alterar o status sem atualizar a data de conclusão.
- **`ddl-auto: validate` + Flyway**: o schema é controlado só pelas migrations.
- **`open-in-view: false`**, para não manter conexão com o banco aberta durante a resposta.

## Segurança das credenciais

Nenhuma senha ou segredo fica no código. A aplicação lê as credenciais de variáveis de ambiente, que localmente ficam no arquivo `.env`:

- `.env` está no `.gitignore` e **nunca** é enviado ao repositório
- `.env.example` mostra quais variáveis existem, sem valores reais
- se alguma variável obrigatória faltar, a aplicação e o Docker Compose param com erro, em vez de subir com um valor padrão inseguro
- os testes usam valores próprios (`application-test.yml`) e um banco descartável criado pelo Testcontainers

## Como rodar

### Pré-requisitos

- Java 21
- Maven 3.9+
- Docker

### 1. Configure as variáveis de ambiente

```bash
cp .env.example .env
```

Edite o `.env` e defina uma senha para o banco e um segredo para o JWT. Para gerar o segredo:

```bash
openssl rand -base64 48
```

### 2. Suba a aplicação

**Opção A: tudo com Docker**

```bash
docker compose up --build
```

**Opção B: banco no Docker e aplicação local (desenvolvimento)**

```bash
docker compose up -d db
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080` e o Swagger em `http://localhost:8080/swagger-ui.html`.

### Acessando o banco pelo DBeaver

Com o container do banco rodando, crie uma conexão PostgreSQL com:

| Campo    | Valor                          |
|----------|--------------------------------|
| Host     | `localhost`                    |
| Porta    | `5432`                         |
| Database | valor de `DB_NAME` no `.env`   |
| Usuário  | valor de `DB_USER` no `.env`   |
| Senha    | valor de `DB_PASSWORD` no `.env` |

As tabelas são criadas pelo Flyway na primeira vez que a aplicação sobe. A tabela `flyway_schema_history` mostra quais migrations já foram aplicadas.

## Testes

```bash
mvn verify
```

| Tipo        | Classe                | O que cobre                                                   |
|-------------|-----------------------|---------------------------------------------------------------|
| Unitário    | `TaskTest`            | Regras de status e conclusão da entidade                      |
| Unitário    | `TaskServiceTest`     | Regras de negócio das tarefas, com repositories mockados      |
| Unitário    | `AuthServiceTest`     | Cadastro e login                                              |
| Unitário    | `TokenServiceTest`    | Geração e validação real do JWT                               |
| Web (slice) | `TaskControllerTest`  | Rotas, segurança, status HTTP e formato dos erros             |
| Integração  | `TaskIntegrationTest` | Fluxo completo com PostgreSQL real, incluindo isolamento entre usuários |

> O teste de integração precisa do Docker rodando.

## Autenticação

1. Cadastre-se em `POST /auth/register`
2. Faça login em `POST /auth/login` e copie o `accessToken`
3. Envie o token no header de todas as outras requisições:

```
Authorization: Bearer <accessToken>
```

No Swagger, use o botão **Authorize** e cole apenas o token.

## Endpoints

| Método | Rota                 | Descrição                  | Autenticação | Sucesso |
|--------|----------------------|----------------------------|--------------|---------|
| POST   | `/auth/register`     | Cadastra usuário           | Não          | 201     |
| POST   | `/auth/login`        | Retorna token JWT          | Não          | 200     |
| GET    | `/tasks`             | Lista tarefas (paginado)   | Sim          | 200     |
| GET    | `/tasks/{id}`        | Busca tarefa por id        | Sim          | 200     |
| POST   | `/tasks`             | Cria tarefa                | Sim          | 201     |
| PUT    | `/tasks/{id}`        | Atualiza dados da tarefa   | Sim          | 200     |
| PATCH  | `/tasks/{id}/status` | Altera o status            | Sim          | 200     |
| DELETE | `/tasks/{id}`        | Remove tarefa              | Sim          | 204     |

### Filtros e paginação

`GET /tasks` aceita:

- `status`: `PENDING`, `IN_PROGRESS` ou `DONE`
- `priority`: `LOW`, `MEDIUM` ou `HIGH`
- `page`: número da página, começando em 0
- `size`: itens por página (padrão 10, máximo 50)
- `sort`: campo e direção, por exemplo `dueDate,asc`

## Exemplos

### Cadastro e login

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Lucas", "email": "lucas@email.com", "password": "minhasenha123"}'

curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "lucas@email.com", "password": "minhasenha123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Criar tarefa

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Authorization: Bearer SEU_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Estudar Spring Boot", "priority": "HIGH", "dueDate": "2026-12-01"}'
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

## Fluxo de branches

- **`main`**: código estável e funcionando, com o CI verde
- **`develop`**: integração das novas funcionalidades

Novas funcionalidades nascem de `develop` em uma branch própria e voltam por Pull Request:

```bash
git checkout develop
git pull
git checkout -b feature/nome-da-funcionalidade
# ... commits ...
git push -u origin feature/nome-da-funcionalidade
# abra um Pull Request para develop
```

Quando `develop` estiver estável, ela é integrada na `main` também por Pull Request.

## Próximos passos

- [ ] Refresh token
- [ ] Rate limit no login
- [ ] Deploy em nuvem
