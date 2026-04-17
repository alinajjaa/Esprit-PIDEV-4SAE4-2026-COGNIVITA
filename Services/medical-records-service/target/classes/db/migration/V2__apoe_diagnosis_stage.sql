-- Flyway Migration V2: APOE status and diagnosis stage columns (safe, nullable additions)
-- These columns were added in Session 1. This migration adds them to existing DBs.

ALTER TABLE medical_records
    ADD COLUMN IF NOT EXISTS apoe_status            VARCHAR(20) DEFAULT 'NOT_TESTED',
    ADD COLUMN IF NOT EXISTS diagnosis_stage         VARCHAR(20) DEFAULT 'PRECLINICAL',
    ADD COLUMN IF NOT EXISTS stage_manually_overridden BOOLEAN DEFAULT FALSE;
