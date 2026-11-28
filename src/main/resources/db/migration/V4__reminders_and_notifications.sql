-- Etapa 3: recordatorios, notificaciones in-app y preferencias

ALTER TABLE users ADD COLUMN notify_events BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN notify_tasks BOOLEAN NOT NULL DEFAULT true;

-- 0 = sin recordatorio; si no, minutos de antelación.
ALTER TABLE event ADD COLUMN reminder_offset_minutes INTEGER;
ALTER TABLE event
    ADD CONSTRAINT ck_event_reminder_offset
        CHECK (reminder_offset_minutes IS NULL OR reminder_offset_minutes BETWEEN 0 AND 20160);

ALTER TABLE task ADD COLUMN reminder_offset_minutes INTEGER;
ALTER TABLE task
    ADD CONSTRAINT ck_task_reminder_offset
        CHECK (reminder_offset_minutes IS NULL OR reminder_offset_minutes BETWEEN 0 AND 20160);

CREATE TABLE reminder (
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type       VARCHAR(10)  NOT NULL,
    target_id         UUID,
    title             VARCHAR(160) NOT NULL,
    body              TEXT,
    trigger_at        TIMESTAMPTZ  NOT NULL,
    channel           VARCHAR(10)  NOT NULL DEFAULT 'in_app',
    status            VARCHAR(10)  NOT NULL DEFAULT 'pending',
    attempts          SMALLINT     NOT NULL DEFAULT 0,
    sent_at           TIMESTAMPTZ,
    idempotency_key   VARCHAR(160) NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_reminder_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_reminder_target CHECK (target_type IN ('task', 'event', 'custom')),
    CONSTRAINT ck_reminder_status CHECK (status IN ('pending', 'sent', 'failed', 'cancelled')),
    CONSTRAINT ck_reminder_channel CHECK (channel IN ('in_app')),
    CONSTRAINT ck_reminder_target_id CHECK (target_type = 'custom' OR target_id IS NOT NULL)
);

CREATE INDEX idx_reminder_due ON reminder (trigger_at) WHERE status = 'pending';
CREATE INDEX idx_reminder_user ON reminder (user_id, trigger_at);
CREATE INDEX idx_reminder_target ON reminder (target_type, target_id) WHERE status = 'pending';

CREATE TABLE notification (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reminder_id UUID REFERENCES reminder (id) ON DELETE SET NULL,
    title       VARCHAR(160) NOT NULL,
    body        TEXT,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user_created ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread ON notification (user_id) WHERE read_at IS NULL;
