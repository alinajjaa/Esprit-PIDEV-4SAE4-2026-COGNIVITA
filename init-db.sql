-- Creates all databases needed by each microservice.
-- MySQL's docker entrypoint runs this once on first startup.

CREATE DATABASE IF NOT EXISTS alzheimer_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS alzheimer_notifications CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS alzheimer_adherence   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant root full access (already default, but explicit for clarity)
GRANT ALL PRIVILEGES ON alzheimer_db.*              TO 'root'@'%';
GRANT ALL PRIVILEGES ON alzheimer_notifications.*   TO 'root'@'%';
GRANT ALL PRIVILEGES ON alzheimer_adherence.*       TO 'root'@'%';
FLUSH PRIVILEGES;
