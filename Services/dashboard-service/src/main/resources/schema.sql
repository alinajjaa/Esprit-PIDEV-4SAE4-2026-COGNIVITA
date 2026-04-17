CREATE TABLE IF NOT EXISTS medical_records (
    id BIGINT PRIMARY KEY,
    risk_level VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS tracking_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(255),
    created_at DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS patient_locations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    latitude DOUBLE,
    longitude DOUBLE,
    `timestamp` DATETIME NOT NULL,
    motion_state VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    medical_record_id BIGINT NOT NULL,
    appointment_type VARCHAR(50),
    scheduled_at DATETIME NOT NULL,
    status VARCHAR(30),
    completed_at DATETIME,
    doctor_name VARCHAR(150),
    specialty VARCHAR(100),
    location VARCHAR(200),
    reminder_sent BOOLEAN DEFAULT FALSE,
    created_at DATETIME,
    CONSTRAINT fk_appointments_medical_record
        FOREIGN KEY (medical_record_id) REFERENCES medical_records(id)
);

CREATE TABLE IF NOT EXISTS mmse_test (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    patient_id BIGINT NOT NULL,
    total_score INT NOT NULL,
    test_date DATE NOT NULL
);
