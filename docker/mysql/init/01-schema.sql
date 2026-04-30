CREATE DATABASE IF NOT EXISTS backend
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE backend;

CREATE TABLE IF NOT EXISTS enterprises (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    business_number VARCHAR(100) NOT NULL,
    industry VARCHAR(255) NOT NULL,
    address VARCHAR(255) NULL,
    foreign_worker_quota INT NULL,
    employment_permit_cert_no VARCHAR(100) NULL,
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
    birth_date DATE NULL,
    phone_number VARCHAR(30) NULL,
    visa_expires_at DATE NULL,
    role VARCHAR(20) NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    visa_expires_at DATE NULL,
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
    case_id VARCHAR(36) NULL,
    code VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    max_uses INT NOT NULL,
    used_count INT NOT NULL,
    active BIT(1) NOT NULL,
    default_role VARCHAR(20) NOT NULL,
    case_id VARCHAR(36) NULL,
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

CREATE TABLE IF NOT EXISTS document_signatures (
    id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    wallet_address VARCHAR(42) NULL,
    chain_id BIGINT NOT NULL,
    verifying_contract VARCHAR(42) NOT NULL,
    typed_data_hash VARCHAR(66) NOT NULL,
    client_typed_data_hash VARCHAR(66) NULL,
    signature TEXT NULL,
    signature_hash VARCHAR(66) NULL,
    nonce VARCHAR(66) NOT NULL,
    deadline DATETIME(6) NOT NULL,
    status VARCHAR(50) NOT NULL,
    signed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_signatures_document_user_wallet (document_id, user_id, wallet_address),
    UNIQUE KEY uk_document_signatures_chain_contract_nonce (chain_id, verifying_contract, nonce),
    UNIQUE KEY uk_document_signatures_typed_data_hash (typed_data_hash),
    UNIQUE KEY uk_document_signatures_signature_hash (signature_hash),
    CONSTRAINT fk_document_signatures_document
        FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_document_signatures_user
        FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_anchors (
    id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    signature_id VARCHAR(36) NOT NULL,
    chain_id BIGINT NOT NULL,
    contract_address VARCHAR(42) NOT NULL,
    anchor_id VARCHAR(66) NOT NULL,
    document_hash VARCHAR(66) NOT NULL,
    case_id_hash VARCHAR(66) NOT NULL,
    tx_hash VARCHAR(66) NULL,
    block_number BIGINT NULL,
    status VARCHAR(50) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    last_error_message VARCHAR(1000) NULL,
    anchored_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_anchors_chain_contract_anchor (chain_id, contract_address, anchor_id),
    UNIQUE KEY uk_document_anchors_tx_hash (tx_hash),
    CONSTRAINT fk_document_anchors_document
        FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT fk_document_anchors_signature
        FOREIGN KEY (signature_id) REFERENCES document_signatures (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_analysis_results (
    id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    extracted_text_hash VARCHAR(64) NULL,
    analysis_result_hash VARCHAR(64) NULL,
    summary TEXT NULL,
    risk_flags TEXT NULL,
    analyzed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_analysis_results_document (document_id),
    CONSTRAINT fk_document_analysis_results_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_extractions (
    id VARCHAR(36) NOT NULL,
    document_id VARCHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    schema_version VARCHAR(100) NOT NULL,
    source_engine VARCHAR(100) NOT NULL,
    source_result_hash VARCHAR(64) NULL,
    extracted_payload TEXT NULL,
    corrected_payload TEXT NULL,
    ai_payload_hash VARCHAR(64) NULL,
    review_required_reason VARCHAR(1000) NULL,
    extracted_at DATETIME(6) NULL,
    corrected_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_extractions_document (document_id),
    CONSTRAINT fk_document_extractions_document
        FOREIGN KEY (document_id) REFERENCES documents (id)
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

CREATE TABLE IF NOT EXISTS consultation_sessions (
    id VARCHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    case_id VARCHAR(36) NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_consultation_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_consultation_sessions_case
        FOREIGN KEY (case_id) REFERENCES cases (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consultation_messages (
    id VARCHAR(36) NOT NULL,
    session_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_consultation_messages_session
        FOREIGN KEY (session_id) REFERENCES consultation_sessions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
