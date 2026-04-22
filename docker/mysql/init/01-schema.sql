CREATE DATABASE IF NOT EXISTS backend
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE backend;

CREATE TABLE IF NOT EXISTS enterprises (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    business_number VARCHAR(100) NOT NULL,
    industry VARCHAR(255) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    enterprise_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT fk_users_enterprise
        FOREIGN KEY (enterprise_id) REFERENCES enterprises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS company_invite_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    enterprise_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    max_uses INT NOT NULL,
    used_count INT NOT NULL,
    active BIT(1) NOT NULL,
    default_role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_company_invite_codes_code (code),
    CONSTRAINT fk_company_invite_codes_enterprise
        FOREIGN KEY (enterprise_id) REFERENCES enterprises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cases (
    id VARCHAR(36) NOT NULL,
    employer_id BIGINT NULL,
    worker_id BIGINT NULL,
    enterprise_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    industry VARCHAR(100) NOT NULL,
    region VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cases_employer
        FOREIGN KEY (employer_id) REFERENCES users (id),
    CONSTRAINT fk_cases_worker
        FOREIGN KEY (worker_id) REFERENCES users (id),
    CONSTRAINT fk_cases_enterprise
        FOREIGN KEY (enterprise_id) REFERENCES enterprises (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS documents (
    id VARCHAR(36) NOT NULL,
    case_id VARCHAR(36) NULL,
    uploader_user_id BIGINT NULL,
    document_type VARCHAR(50) NULL,
    original_file_name VARCHAR(255) NULL,
    storage_key VARCHAR(255) NULL,
    mime_type VARCHAR(255) NULL,
    file_size BIGINT NULL,
    sha256_hash VARCHAR(64) NULL,
    anchored_tx_id VARCHAR(255) NULL,
    status VARCHAR(50) NOT NULL,
    issued_at DATE NULL,
    expires_at DATE NULL,
    ocr_completed_at DATETIME(6) NULL,
    analyzed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_documents_case
        FOREIGN KEY (case_id) REFERENCES cases (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alerts (
    id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(30) NOT NULL,
    is_read BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alerts_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS dashboards (
    id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_dashboards_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS case_checklist_statuses (
    id VARCHAR(36) NOT NULL,
    case_id VARCHAR(36) NOT NULL,
    checklist_item_code VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_case_checklist_statuses_case_item_code (case_id, checklist_item_code),
    CONSTRAINT fk_case_checklist_statuses_case
        FOREIGN KEY (case_id) REFERENCES cases (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consultations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    diagnose VARCHAR(1000) NOT NULL,
    uid BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_consultations_user
        FOREIGN KEY (uid) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
