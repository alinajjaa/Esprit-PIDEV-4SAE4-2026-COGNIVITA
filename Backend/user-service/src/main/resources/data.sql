-- Seed users for embedded DB (H2). This helps Admin Dashboard show users in dev mode.
-- If you run with MySQL, Spring Boot doesn't run this by default (non-embedded DB).

INSERT INTO users (email, first_name, last_name, password, phone, role, active, created_at)
VALUES ('admin@alzheimer.com', 'Admin', 'User', 'admin123', NULL, 'ADMIN', TRUE, CURRENT_TIMESTAMP);

INSERT INTO users (email, first_name, last_name, password, phone, role, active, created_at)
VALUES ('doctor@alzheimer.com', 'Dr', 'Smith', 'doctor123', NULL, 'DOCTOR', TRUE, CURRENT_TIMESTAMP);

INSERT INTO users (email, first_name, last_name, password, phone, role, active, created_at)
VALUES ('patient1@alzheimer.com', 'John', 'Doe', 'patient123', NULL, 'USER', TRUE, CURRENT_TIMESTAMP);

INSERT INTO users (email, first_name, last_name, password, phone, role, active, created_at)
VALUES ('patient2@alzheimer.com', 'Jane', 'Smith', 'patient123', NULL, 'USER', TRUE, CURRENT_TIMESTAMP);

