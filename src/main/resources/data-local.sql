insert into enterprises (name, business_number, industry, country, status, created_at, updated_at)
select 'Local Demo Company',
       'LOCAL-BIZ-001',
       'Manufacturing',
       'Korea',
       'ACTIVE',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from enterprises
    where business_number = 'LOCAL-BIZ-001'
);

insert into users (email, password_hash, name, role, user_type, status, country_id, enterprise_id, created_at, updated_at)
select 'admin.local@kworkerharmony.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiB6H4T6k9KuX3sI5Yucs5cjox96D65',
       'Local Admin',
       'ADMIN',
       'EMPLOYER',
       'ACTIVE',
       (select id from countries where country_code = 'KR'),
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from users
    where email = 'admin.local@kworkerharmony.com'
);

insert into users (email, password_hash, name, role, user_type, status, country_id, enterprise_id, created_at, updated_at)
select 'employer.local@kworkerharmony.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiB6H4T6k9KuX3sI5Yucs5cjox96D65',
       'Local Employer',
       'EMPLOYER',
       'EMPLOYER',
       'ACTIVE',
       (select id from countries where country_code = 'KR'),
       (select id from enterprises where business_number = 'LOCAL-BIZ-001'),
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from users
    where email = 'employer.local@kworkerharmony.com'
);

insert into users (email, password_hash, name, role, user_type, status, country_id, enterprise_id, created_at, updated_at)
select 'worker.local@kworkerharmony.com',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiB6H4T6k9KuX3sI5Yucs5cjox96D65',
       'Local Worker',
       'WORKER',
       'WORKER',
       'ACTIVE',
       (select id from countries where country_code = 'KR'),
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

insert into case_checklist_items (id, case_id, checklist_item_id, status, note, created_at, updated_at)
select '22222222-2222-2222-2222-222222222221',
       '11111111-1111-1111-1111-111111111111',
       '00000000-0000-0000-0000-000000000001',
       'COMPLETED',
       'Passport copy verified in local environment.',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from case_checklist_items
    where id = '22222222-2222-2222-2222-222222222221'
);

insert into case_checklist_items (id, case_id, checklist_item_id, status, note, created_at, updated_at)
select '22222222-2222-2222-2222-222222222222',
       '11111111-1111-1111-1111-111111111111',
       '00000000-0000-0000-0000-000000000002',
       'IN_PROGRESS',
       'Contract upload pending signature review.',
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from case_checklist_items
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
