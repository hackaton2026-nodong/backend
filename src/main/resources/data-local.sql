insert into enterprises (name, business_number, industry, country_code, language_code, status, created_at, updated_at)
select 'Local Demo Company',
       'LOCAL-BIZ-001',
       'Manufacturing',
       'KR',
       'ko',
       'ACTIVE',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from enterprises
    where business_number = 'LOCAL-BIZ-001'
);

insert into users (email, password_hash, name, role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at)
select 'admin.local@kworkerharmony.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiB6H4T6k9KuX3sI5Yucs5cjox96D65',
       'Local Admin',
       'ADMIN',
       'EMPLOYER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from users
    where email = 'admin.local@kworkerharmony.com'
);

insert into users (email, password_hash, name, role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at)
select 'employer.local@kworkerharmony.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiB6H4T6k9KuX3sI5Yucs5cjox96D65',
       'Local Employer',
       'EMPLOYER',
       'EMPLOYER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from users
    where email = 'employer.local@kworkerharmony.com'
);

insert into users (email, password_hash, name, role, user_type, status, country_code, language_code, enterprise_id, created_at, updated_at)
select 'worker.local@kworkerharmony.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiB6H4T6k9KuX3sI5Yucs5cjox96D65',
       'Local Worker',
       'WORKER',
       'WORKER',
       'ACTIVE',
       'KR',
       'ko',
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from users
    where email = 'worker.local@kworkerharmony.com'
);

insert into cases (id, employer_id, worker_id, enterprise_id, status, industry, region, created_at, updated_at)
select '11111111-1111-1111-1111-111111111111',
       (select id from users where email = 'employer.local@kworkerharmony.com'),
       (select id from users where email = 'worker.local@kworkerharmony.com'),
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       'ACTIVE',
       'Manufacturing',
       'Seoul',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from cases
    where id = '11111111-1111-1111-1111-111111111111'
);

insert into case_checklist_statuses (id, case_id, checklist_item_code, status, note, created_at, updated_at)
select '22222222-2222-2222-2222-222222222221',
       '11111111-1111-1111-1111-111111111111',
       'EPS_PRE_APPROVAL',
       'COMPLETED',
       '고용허가 사전 절차 확인 완료.',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from case_checklist_statuses
    where id = '22222222-2222-2222-2222-222222222221'
);

insert into case_checklist_statuses (id, case_id, checklist_item_code, status, note, created_at, updated_at)
select '22222222-2222-2222-2222-222222222222',
       '11111111-1111-1111-1111-111111111111',
       'LABOR_WORK_CONTRACT',
       'IN_PROGRESS',
       '근로계약 및 교부 문서 검토 진행 중.',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from case_checklist_statuses
    where id = '22222222-2222-2222-2222-222222222222'
);

insert into alerts (id, user_id, title, message, type, is_read, created_at, updated_at)
select '33333333-3333-3333-3333-333333333331',
       (select id from users where email = 'worker.local@kworkerharmony.com'),
       'Document review pending',
       'Your employment contract is waiting for review.',
       'CHECKLIST',
       false,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from alerts
    where id = '33333333-3333-3333-3333-333333333331'
);
