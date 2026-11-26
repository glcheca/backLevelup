-- Etapa 1: núcleo académico (cursos, horario, eventos y tareas)

CREATE TABLE course (
    id         UUID PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(80)  NOT NULL,
    code       VARCHAR(20),
    professor  VARCHAR(120),
    color      VARCHAR(7)   NOT NULL DEFAULT '#6366f1',
    credits    NUMERIC(4, 1),
    period     VARCHAR(7)   NOT NULL,
    archived   BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_course_user_name_period UNIQUE (user_id, name, period),
    CONSTRAINT ck_course_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_course_credits CHECK (credits IS NULL OR (credits >= 0 AND credits <= 30))
);

CREATE INDEX idx_course_user ON course (user_id) WHERE archived = false;

CREATE TABLE class_block (
    id          UUID PRIMARY KEY,
    course_id   UUID        NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    day_of_week SMALLINT    NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    room        VARCHAR(60),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_class_block_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_class_block_time CHECK (end_time > start_time)
);

CREATE INDEX idx_class_block_course ON class_block (course_id);

CREATE TABLE event (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id   UUID REFERENCES course (id) ON DELETE SET NULL,
    title       VARCHAR(160) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL DEFAULT 'personal',
    start_at    TIMESTAMPTZ  NOT NULL,
    end_at      TIMESTAMPTZ  NOT NULL,
    all_day     BOOLEAN      NOT NULL DEFAULT false,
    location    VARCHAR(160),
    status      VARCHAR(20)  NOT NULL DEFAULT 'scheduled',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_event_range CHECK (end_at >= start_at),
    CONSTRAINT ck_event_type CHECK (type IN ('exam', 'assignment', 'class', 'personal')),
    CONSTRAINT ck_event_status CHECK (status IN ('scheduled', 'done', 'cancelled'))
);

CREATE INDEX idx_event_user_range ON event (user_id, start_at, end_at);

CREATE TABLE task (
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id     UUID REFERENCES course (id) ON DELETE SET NULL,
    event_id      UUID REFERENCES event (id) ON DELETE SET NULL,
    title         VARCHAR(160) NOT NULL,
    description   TEXT,
    status        VARCHAR(10)  NOT NULL DEFAULT 'todo',
    priority      SMALLINT     NOT NULL DEFAULT 1,
    due_at        TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    subtasks_json VARCHAR(4000) NOT NULL DEFAULT '[]',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_task_status CHECK (status IN ('todo', 'doing', 'done')),
    CONSTRAINT ck_task_priority CHECK (priority BETWEEN 0 AND 2)
);

CREATE INDEX idx_task_user_due ON task (user_id, due_at) WHERE status <> 'done';
