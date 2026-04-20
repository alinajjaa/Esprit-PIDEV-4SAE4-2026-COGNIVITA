-- Flyway Migration V3: Medications and Care Notes tables

CREATE TABLE IF NOT EXISTS medications (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    medical_record_id BIGINT       NOT NULL,
    name              VARCHAR(200) NOT NULL,
    dosage            VARCHAR(100),
    frequency         VARCHAR(100),
    prescribed_by     VARCHAR(150),
    start_date        DATE,
    end_date          DATE,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    risk_flag         VARCHAR(50),
    notes             TEXT,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME,
    INDEX idx_med_record_id (medical_record_id),
    INDEX idx_med_is_active (is_active),
    CONSTRAINT fk_med_record FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS care_notes (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    medical_record_id BIGINT       NOT NULL,
    author_name       VARCHAR(150) NOT NULL,
    author_role       VARCHAR(100),
    content           TEXT         NOT NULL,
    note_type         VARCHAR(50),
    is_pinned         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME,
    INDEX idx_note_record_id (medical_record_id),
    INDEX idx_note_is_pinned (is_pinned),
    CONSTRAINT fk_note_record FOREIGN KEY (medical_record_id)
        REFERENCES medical_records(id) ON DELETE CASCADE
);
