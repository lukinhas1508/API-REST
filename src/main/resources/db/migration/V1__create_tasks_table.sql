CREATE TABLE tasks (
    id           BIGSERIAL    PRIMARY KEY,
    title        VARCHAR(100) NOT NULL,
    description  VARCHAR(500),
    status       VARCHAR(20)  NOT NULL,
    priority     VARCHAR(20)  NOT NULL,
    due_date     DATE,
    completed_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_priority ON tasks (priority);
