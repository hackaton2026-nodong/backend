alter table enterprises
    add column address varchar(255) null;

alter table enterprises
    add column foreign_worker_quota int null;

alter table enterprises
    add column employment_permit_cert_no varchar(100) null;

alter table users
    add column birth_date date null;

alter table users
    add column phone_number varchar(30) null;

alter table users
    add column visa_expires_at date null;

alter table documents
    add column ocr_completed_at datetime(6) null;

alter table documents
    add column analyzed_at datetime(6) null;

alter table document_analysis_results
    add column issue_candidates text null;

alter table document_analysis_results
    add column generated_analysis text null;

alter table document_analysis_results
    add column findings text null;

alter table document_analysis_results
    add column field_findings text null;

alter table document_analysis_results
    add column citations text null;

alter table document_analysis_results
    add column recommended_actions text null;

alter table document_analysis_results
    add column related_institutions text null;

alter table document_analysis_results
    add column case_status text null;

alter table document_analysis_results
    add column detail_json longtext null;

alter table document_analysis_results
    add column failed_reason text null;

alter table document_analysis_results
    modify column risk_flags longtext null,
    modify column issue_candidates longtext null,
    modify column generated_analysis longtext null,
    modify column findings longtext null,
    modify column field_findings longtext null,
    modify column citations longtext null,
    modify column recommended_actions longtext null,
    modify column related_institutions longtext null,
    modify column detail_json longtext null;

alter table company_invite_codes
    add column case_id varchar(36) null;

create table if not exists document_signatures (
    id varchar(36) not null,
    document_id varchar(36) not null,
    user_id bigint not null,
    wallet_address varchar(42) null,
    chain_id bigint not null,
    verifying_contract varchar(42) not null,
    typed_data_hash varchar(66) not null,
    client_typed_data_hash varchar(66) null,
    signature text null,
    signature_hash varchar(66) null,
    nonce varchar(66) not null,
    deadline datetime(6) not null,
    status varchar(50) not null,
    signed_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_document_signatures_document_user_wallet (document_id, user_id, wallet_address),
    unique key uk_document_signatures_chain_contract_nonce (chain_id, verifying_contract, nonce),
    unique key uk_document_signatures_typed_data_hash (typed_data_hash),
    unique key uk_document_signatures_signature_hash (signature_hash),
    constraint fk_document_signatures_document
        foreign key (document_id) references documents (id),
    constraint fk_document_signatures_user
        foreign key (user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists document_anchors (
    id varchar(36) not null,
    document_id varchar(36) not null,
    signature_id varchar(36) not null,
    chain_id bigint not null,
    contract_address varchar(42) not null,
    anchor_id varchar(66) not null,
    document_hash varchar(66) not null,
    case_id_hash varchar(66) not null,
    tx_hash varchar(66) null,
    block_number bigint null,
    status varchar(50) not null,
    retry_count int not null default 0,
    last_error_message varchar(1000) null,
    anchored_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_document_anchors_chain_contract_anchor (chain_id, contract_address, anchor_id),
    unique key uk_document_anchors_tx_hash (tx_hash),
    constraint fk_document_anchors_document
        foreign key (document_id) references documents (id),
    constraint fk_document_anchors_signature
        foreign key (signature_id) references document_signatures (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists document_extractions (
    id varchar(36) not null,
    document_id varchar(36) not null,
    status varchar(50) not null,
    schema_version varchar(100) not null,
    source_engine varchar(100) not null,
    source_result_hash varchar(64) null,
    extracted_payload text null,
    corrected_payload text null,
    ai_payload_hash varchar(64) null,
    review_required_reason varchar(1000) null,
    extracted_at datetime(6) null,
    corrected_at datetime(6) null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    unique key uk_document_extractions_document (document_id),
    constraint fk_document_extractions_document
        foreign key (document_id) references documents (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists consultations (
    id bigint not null auto_increment,
    diagnose varchar(1000) not null,
    uid bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint fk_consultations_user
        foreign key (uid) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists consultation_sessions (
    id varchar(36) not null,
    title varchar(100) not null,
    case_id varchar(36) null,
    user_id bigint not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint fk_consultation_sessions_user
        foreign key (user_id) references users (id),
    constraint fk_consultation_sessions_case
        foreign key (case_id) references cases (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table if not exists consultation_messages (
    id varchar(36) not null,
    session_id varchar(36) not null,
    role varchar(20) not null,
    content varchar(4000) not null,
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    primary key (id),
    constraint fk_consultation_messages_session
        foreign key (session_id) references consultation_sessions (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
