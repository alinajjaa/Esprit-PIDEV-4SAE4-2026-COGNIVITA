-- Flyway Migration V1: Appointments and Audit Log tables
-- Applied when spring.flyway.enabled=true

-- Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    medical_record_id BIGINT       NOT NULL,
    doctor_name       VARCHAR(150),
    specialty         VARCHAR(100),
    appointment_type  VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    scheduled_at      DATETIME     NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'SCHEDULED',
    location          VARCHAR(200),
    notes             TEXT,
    completed_at      DATETIME,
    reminder_sent     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME,
    INDEX idx_appt_record_id    (medical_record_id),
    INDEX idx_appt_scheduled_at (scheduled_at),
    INDEX idx_appt_status       (status),
    CONSTRAINT fk_appt_record FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id) ON DELETE CASCADE
);

-- Audit log table (Session 4)
CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     BIGINT       NOT NULL,
    action        VARCHAR(50)  NOT NULL,   -- CREATE, UPDATE, DELETE, VIEW
    performed_by  VARCHAR(200),
    ip_address    VARCHAR(50),
    changes_json  TEXT,
    occurred_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_entity    (entity_type, entity_id),
    INDEX idx_audit_occurred  (occurred_at DESC),
    INDEX idx_audit_performed (performed_by)
);
