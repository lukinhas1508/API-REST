-- Tarefas criadas antes da autenticação não têm dono, então são descartadas
-- para permitir a coluna user_id como obrigatória.
DELETE FROM tasks;

ALTER TABLE tasks ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
