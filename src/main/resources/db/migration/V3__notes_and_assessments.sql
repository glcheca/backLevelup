-- Etapa 2: apuntes (markdown) y calificaciones ponderadas

CREATE TABLE note (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id   UUID REFERENCES course (id) ON DELETE SET NULL,
    title       VARCHAR(160) NOT NULL,
    content_md  TEXT         NOT NULL DEFAULT '',
    pinned      BOOLEAN      NOT NULL DEFAULT false,
    search_text TEXT         NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_note_user_updated ON note (user_id, updated_at DESC);
CREATE INDEX idx_note_course ON note (course_id);

-- Etiquetas normalizadas: funciona igual en PostgreSQL y en H2 (tests).
CREATE TABLE note_tag (
    note_id UUID        NOT NULL REFERENCES note (id) ON DELETE CASCADE,
    tag     VARCHAR(40) NOT NULL,
    PRIMARY KEY (note_id, tag)
);

CREATE INDEX idx_note_tag_tag ON note_tag (tag);

CREATE TABLE assessment (
    id         UUID PRIMARY KEY,
    course_id  UUID          NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    name       VARCHAR(120)  NOT NULL,
    weight     NUMERIC(5, 2) NOT NULL,
    score      NUMERIC(5, 2),
    max_score  NUMERIC(5, 2) NOT NULL,
    due_at     TIMESTAMPTZ,
    notes      TEXT,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_assessment_weight CHECK (weight > 0 AND weight <= 100),
    CONSTRAINT ck_assessment_max_score CHECK (max_score > 0),
    CONSTRAINT ck_assessment_score CHECK (score IS NULL OR (score >= 0 AND score <= max_score))
);

CREATE INDEX idx_assessment_course ON assessment (course_id);
