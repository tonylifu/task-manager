-- V1__create_tasks_table.sql
-- Task Manager - Initial Schema

CREATE TABLE IF NOT EXISTS tasks (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    title       VARCHAR(255)    NOT NULL,
    description TEXT,
    status      VARCHAR(50)     NOT NULL,
    due_date    TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version     BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_tasks PRIMARY KEY (id),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO','IN_PROGRESS','ON_HOLD','DONE','CANCELLED')),
    CONSTRAINT chk_tasks_title_length CHECK (char_length(title) >= 1)
);

CREATE INDEX IF NOT EXISTS idx_tasks_status     ON tasks(status);
CREATE INDEX IF NOT EXISTS idx_tasks_due_date   ON tasks(due_date);
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks(created_at DESC);

COMMENT ON TABLE  tasks              IS 'Core task entity table';
COMMENT ON COLUMN tasks.id          IS 'Unique task identifier (UUID v4)';
COMMENT ON COLUMN tasks.title       IS 'Task title (required, max 255 chars)';
COMMENT ON COLUMN tasks.description IS 'Optional task description (max 5000 chars)';
COMMENT ON COLUMN tasks.status      IS 'Task lifecycle status';
COMMENT ON COLUMN tasks.due_date    IS 'Optional due date stored as UTC timestamp with timezone';
COMMENT ON COLUMN tasks.version     IS 'Optimistic locking version counter';
