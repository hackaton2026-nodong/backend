USE backend;

INSERT INTO countries (country_code, name, created_at, updated_at)
SELECT 'KR', 'Korea', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM countries
    WHERE country_code = 'KR'
);
