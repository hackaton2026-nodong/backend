insert into enterprises (
    name, business_number, industry, address, foreign_worker_quota,
    employment_permit_cert_no, country_code, language_code, status, created_at, updated_at
)
select '한국제조',
       '214-86-73951',
       '제조업 / 금속부품 가공',
       '경기도 안산시 단원구 산단로 125, 한국제조',
       5,
       'EPS-2026-KM-0317',
       'KR',
       'ko',
       'ACTIVE',
       current_timestamp,
       current_timestamp
where not exists (
    select 1 from enterprises where business_number = '214-86-73951'
);

insert into users (
    email, password_hash, name, birth_date, phone_number, visa_expires_at,
    role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at
)
select 'minsukim@hankukmanufacturing.co.kr',
       '$2a$10$jUJLnbeDwaNvtNoILkT7dOIC/CPeUKhsGoMa3/ZARc10xrK/kS5.2',
       '김민수',
       null,
       '010-4821-7395',
       null,
       'ADMIN',
       'EMPLOYER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = '214-86-73951'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1 from users where email = 'minsukim@hankukmanufacturing.co.kr'
);

insert into users (
    email, password_hash, name, birth_date, phone_number, visa_expires_at,
    role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at
)
select email,
       '$2a$10$jUJLnbeDwaNvtNoILkT7dOIC/CPeUKhsGoMa3/ZARc10xrK/kS5.2',
       name,
       birth_date,
       phone_number,
       visa_expires_at,
       'WORKER',
       'WORKER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = '214-86-73951'),
       current_timestamp,
       current_timestamp
from (
    select 1 as n, 'minh.nguyen97@example.com' as email, 'NGUYEN VAN MINH' as name, date '1997-04-12' as birth_date, '010-7314-2568' as phone_number, date '2027-08-14' as visa_expires_at
    union all select 2, 'somchai.phanit95@example.com', 'SOMCHAI PHANIT', date '1995-11-03', '010-8492-1176', date '2027-10-02'
    union all select 3, 'maria.santos98@example.com', 'MARIA LUZ SANTOS', date '1998-07-21', '010-6258-9043', date '2028-01-19'
    union all select 4, 'dewi.lestari96@example.com', 'DEWI LESTARI', date '1996-02-18', '010-3729-6815', date '2027-06-25'
    union all select 5, 'ram.thapa94@example.com', 'RAM BAHADUR THAPA', date '1994-09-09', '010-9184-5632', date '2028-03-11'
) seed_workers
where not exists (
    select 1 from users where email = seed_workers.email
);

insert into cases (id, employer_id, worker_id, enterprise_id, status, industry, region, created_at, updated_at)
select concat('11111111-1111-1111-1111-11111111111', n),
       (select id from users where email = 'minsukim@hankukmanufacturing.co.kr'),
       (select id from users where email = worker_email),
       (select id from enterprises where business_number = '214-86-73951'),
       'ACTIVE',
       '제조업 / 금속부품 가공',
       '경기도 안산시',
       current_timestamp,
       current_timestamp
from (
    select 1 as n, 'minh.nguyen97@example.com' as worker_email
    union all select 2, 'somchai.phanit95@example.com'
    union all select 3, 'maria.santos98@example.com'
    union all select 4, 'dewi.lestari96@example.com'
    union all select 5, 'ram.thapa94@example.com'
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
       (select id from users where email = 'minsukim@hankukmanufacturing.co.kr'),
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
select (select id from enterprises where business_number = '214-86-73951'),
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
