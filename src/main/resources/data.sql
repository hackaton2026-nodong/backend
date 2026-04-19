insert into countries (country_code, name, created_at, updated_at)
select 'KR', 'Korea', current_timestamp, current_timestamp
where not exists (
    select 1
    from countries
    where country_code = 'KR'
);

insert into checklist_items (id, code, title, description, required, created_at, updated_at)
select '00000000-0000-0000-0000-000000000001',
       'PASSPORT_COPY',
       'Passport Copy',
       'Upload a copy of the worker passport.',
       true,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from checklist_items
    where code = 'PASSPORT_COPY'
);

insert into checklist_items (id, code, title, description, required, created_at, updated_at)
select '00000000-0000-0000-0000-000000000002',
       'EMPLOYMENT_CONTRACT',
       'Employment Contract',
       'Upload the signed employment contract.',
       true,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from checklist_items
    where code = 'EMPLOYMENT_CONTRACT'
);

insert into checklist_items (id, code, title, description, required, created_at, updated_at)
select '00000000-0000-0000-0000-000000000003',
       'RESIDENCE_CARD',
       'Residence Card',
       'Upload the residence card after issuance.',
       false,
       current_timestamp,
       current_timestamp
where not exists (
    select 1
    from checklist_items
    where code = 'RESIDENCE_CARD'
);
