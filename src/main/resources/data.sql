insert into countries (country_code, name, created_at, updated_at)
select 'KR', 'Korea', current_timestamp, current_timestamp
where not exists (
    select 1
    from countries
    where country_code = 'KR'
);
