insert into enterprises (
    name, business_number, industry, address, foreign_worker_quota,
    employment_permit_cert_no, country_code, language_code, status, created_at, updated_at
)
select 'Kohamo Demo Factory',
       'LOCAL-BIZ-001',
       'Manufacturing',
       'Seoul, Guro-gu Digital-ro 100',
       5,
       'EPS-DEMO-2026-001',
       'KR',
       'ko',
       'ACTIVE',
       current_timestamp,
       current_timestamp
where not exists (
    select 1 from enterprises where business_number = 'LOCAL-BIZ-001'
);

insert into users (
    email, password_hash, name, birth_date, phone_number, visa_expires_at,
    role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at
)
select 'employer.local@kohamo.com',
       '$2a$10$jUJLnbeDwaNvtNoILkT7dOIC/CPeUKhsGoMa3/ZARc10xrK/kS5.2',
       'Kim Employer',
       null,
       '010-1000-0000',
       null,
       'ADMIN',
       'EMPLOYER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1 from users where email = 'employer.local@kohamo.com'
);

insert into users (
    email, password_hash, name, birth_date, phone_number, visa_expires_at,
    role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at
)
select concat('worker', n, '.local@kohamo.com'),
       '$2a$10$jUJLnbeDwaNvtNoILkT7dOIC/CPeUKhsGoMa3/ZARc10xrK/kS5.2',
       concat('Demo Worker ', n),
       date_add(current_date, interval -(25 + n) year),
       concat('010-2000-000', n),
       date_add(current_date, interval (6 + n) month),
       'WORKER',
       'WORKER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
from (
    select 1 as n union all select 2 union all select 3 union all select 4 union all select 5
) seed_workers
where not exists (
    select 1 from users where email = concat('worker', n, '.local@kohamo.com')
);

insert into cases (id, employer_id, worker_id, enterprise_id, status, industry, region, created_at, updated_at)
select concat('11111111-1111-1111-1111-11111111111', n),
       (select id from users where email = 'employer.local@kohamo.com'),
       (select id from users where email = concat('worker', n, '.local@kohamo.com')),
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       'ACTIVE',
       'Manufacturing',
       'Seoul',
       current_timestamp,
       current_timestamp
from (
    select 1 as n union all select 2 union all select 3 union all select 4 union all select 5
) seed_cases
where not exists (
    select 1 from cases where id = concat('11111111-1111-1111-1111-11111111111', n)
);

insert into documents (
    id, case_id, uploader_user_id, document_type, original_file_name, storage_key,
    mime_type, file_size, sha256_hash, anchored_tx_id, status, issued_at, expires_at,
    ocr_completed_at, analyzed_at, created_at, updated_at
)
select concat('44444444-4444-4444-4444-44444444444', n),
       concat('11111111-1111-1111-1111-11111111111', n),
       (select id from users where email = 'employer.local@kohamo.com'),
       'EMPLOYMENT_CONTRACT',
       concat('employment-contract-worker-', n, '.pdf'),
       concat('local/contracts/worker-', n, '.pdf'),
       'application/pdf',
       102400 + n,
       lpad(hex(n), 64, '0'),
       null,
       'ANALYZED',
       current_date,
       date_add(current_date, interval 1 year),
       current_timestamp,
       current_timestamp,
       current_timestamp,
       current_timestamp
from (
    select 1 as n union all select 2 union all select 3 union all select 4 union all select 5
) seed_documents
where not exists (
    select 1 from documents where id = concat('44444444-4444-4444-4444-44444444444', n)
);

insert into document_analysis_results (
    id, document_id, status, extracted_text_hash, analysis_result_hash,
    summary, risk_flags, analyzed_at, created_at, updated_at
)
select concat('55555555-5555-5555-5555-55555555555', n),
       concat('44444444-4444-4444-4444-44444444444', n),
       'COMPLETED',
       lpad(hex(100 + n), 64, '0'),
       lpad(hex(200 + n), 64, '0'),
       concat('Seed employment contract analysis for worker ', n, '.'),
       '[]',
       current_timestamp,
       current_timestamp,
       current_timestamp
from (
    select 1 as n union all select 2 union all select 3 union all select 4 union all select 5
) seed_analysis
where not exists (
    select 1 from document_analysis_results where id = concat('55555555-5555-5555-5555-55555555555', n)
);

insert into company_invite_codes (
    enterprise_id, case_id, code, expires_at, max_uses, used_count,
    active, default_role, created_at, updated_at
)
select (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       concat('11111111-1111-1111-1111-11111111111', n),
       concat('KOHAMO-WORKER-', n),
       date_add(current_timestamp, interval 1 year),
       1,
       1,
       true,
       'WORKER',
       current_timestamp,
       current_timestamp
from (
    select 1 as n union all select 2 union all select 3 union all select 4 union all select 5
) seed_invites
where not exists (
    select 1 from company_invite_codes where code = concat('KOHAMO-WORKER-', n)
);
