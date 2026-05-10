SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

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
       country_code,
       language_code,
       (select id from enterprises where business_number = '214-86-73951'),
       current_timestamp,
       current_timestamp
from (
    select 1 as n, 'minh.nguyen97@example.com' as email, 'NGUYEN VAN MINH' as name, date '1997-04-12' as birth_date, '010-7314-2568' as phone_number, date '2027-08-14' as visa_expires_at, 'VN' as country_code, 'vi' as language_code
    union all select 2, 'somchai.phanit95@example.com', 'SOMCHAI PHANIT', date '1995-11-03', '010-8492-1176', date '2027-10-02', 'TH', 'th'
    union all select 3, 'maria.santos98@example.com', 'MARIA LUZ SANTOS', date '1998-07-21', '010-6258-9043', date '2028-01-19', 'PH', 'fil'
    union all select 4, 'dewi.lestari96@example.com', 'DEWI LESTARI', date '1996-02-18', '010-3729-6815', date '2027-06-25', 'ID', 'id'
    union all select 5, 'ram.thapa94@example.com', 'RAM BAHADUR THAPA', date '1994-09-09', '010-9184-5632', date '2028-03-11', 'NP', 'ne'
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

insert into document_extractions (
    id, document_id, status, schema_version, source_engine, source_result_hash,
    extracted_payload, corrected_payload, ai_payload_hash, review_required_reason,
    extracted_at, corrected_at, created_at, updated_at
)
select concat('66666666-6666-6666-6666-66666666666', n),
       concat('44444444-4444-4444-4444-44444444444', n),
       'EXTRACTED',
       'employment-contract-v1',
       'seed-structured-contract',
       lpad(hex(300 + n), 64, '0'),
       concat(
           '{"contractTerms":{"contractPeriod":{"contractStartDate":"', current_date, '","contractEndDate":"', date_add(current_date, interval 1 year), '"},',
           '"wage":{"amount":', wage_amount, ',"basePay":', base_pay, ',"currency":"KRW","period":"MONTHLY","paymentDay":10,"paymentMethod":"BANK_TRANSFER","overtimeNightHolidayPremiumMentioned":', overtime_mentioned, '},',
           '"workingHours":{"startTime":"', start_time, '","endTime":"', end_time, '","hoursPerDay":8,"hoursPerWeek":40,"maxVariableHoursPerDay":', variable_hours, '},',
           '"breakTime":{"minutesPerDay":60},',
           '"holidays":{"legalHolidayPaid":true,"otherHoliday":', other_holiday, '},',
           '"dormitory":{"provided":', dormitory_provided, ',"typeCategory":"DORMITORY","deductionAmount":', dormitory_deduction, '},',
           '"meals":{"provided":true,"deductionAmount":', meal_deduction, ',"providedMeals":["LUNCH"]},',
           '"work":{"industryCategory":"MANUFACTURING","jobCategory":"METAL_PARTS","workplaceRegion":"경기도 안산시"}}}'
       ),
       null,
       lpad(hex(400 + n), 64, '0'),
       null,
       current_timestamp,
       null,
       current_timestamp,
       current_timestamp
from (
    select 1 as n, 2600000 as wage_amount, 2450000 as base_pay, 0 as dormitory_deduction, 0 as meal_deduction, '08:30' as start_time, '17:30' as end_time, 2 as variable_hours, 'true' as overtime_mentioned, 'false' as other_holiday, 'false' as dormitory_provided
    union all select 2, 2500000, 2300000, 200000, 0, '09:00', '18:00', 3, 'false', 'false', 'true'
    union all select 3, 2400000, 2300000, 100000, 0, '08:00', '17:00', 2, 'true', 'true', 'true'
    union all select 4, 2550000, 2400000, 150000, 50000, '08:30', '17:30', 1, 'true', 'false', 'true'
    union all select 5, 2700000, 2600000, 0, 0, '08:00', '17:00', 2, 'false', 'false', 'false'
) seed_extractions
where not exists (
    select 1 from document_extractions where document_id = concat('44444444-4444-4444-4444-44444444444', n)
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
       '계약서 분석 요약을 준비 중입니다.',
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

update document_analysis_results
set summary = '연장근로 가능성이 있는 계약입니다. 근로시간과 연장근로수당 산정 기준을 서명 전 확인해야 합니다.',
    risk_flags = '[{"code":"OVERTIME_PAY_RULE_REVIEW","label":"연장근로수당 기준 확인 필요","level":"중간","description":"계약서에 연장근로 발생 시 가산수당 산정 기준이 충분히 드러나지 않습니다."}]',
    issue_candidates = '["OVERTIME_PAY","WORKING_HOURS"]',
    generated_analysis = '{"status":"COMPLETED","text":"개인정보를 제외한 요약 기준으로 보면, 이 계약서는 기본 계약기간과 임금 정보는 확인되지만 연장근로가 발생했을 때 어떤 기준으로 수당을 지급하는지 추가 확인이 필요합니다. 서명 전 근로시간 산정표와 연장근로수당 지급 기준을 문서로 보완하는 것이 좋습니다."}',
    findings = '[{"id":"finding-overtime-rule","title":"연장근로수당 기준 확인","description":"주 40시간을 넘는 근로가 발생할 수 있다면 통상임금 50% 이상 가산 기준이 계약서나 별도 문서에 명확해야 합니다.","severity":"중간","fieldName":"overtimePay"},{"id":"finding-working-hours","title":"근로시간 산정 기준 확인","description":"출퇴근, 휴게, 대기시간이 실제 근로시간에 포함되는지 확인해야 합니다.","severity":"중간","fieldName":"workingHours"}]',
    citations = '[{"id":"law-lsa-50","type":"LAW","title":"근로기준법 제50조 근로시간","sourceUrl":"https://www.law.go.kr/법령/근로기준법/제50조"},{"id":"law-lsa-56","type":"LAW","title":"근로기준법 제56조 연장·야간 및 휴일 근로","sourceUrl":"https://www.law.go.kr/법령/근로기준법/제56조"},{"id":"guide-overtime","type":"GUIDE","title":"고용노동 가이드북 연장·야간 및 휴일근로"}]',
    recommended_actions = '[{"id":"action-confirm-overtime","label":"연장근로수당 기준 보완 요청","description":"서명 전 연장근로수당 산정 기준과 지급일을 문서로 확인하세요.","priority":"중간","institutionName":"고용노동부 고객상담센터","expectedPath":"사용자에게 보완 요청 -> 미해결 시 상담"}]',
    related_institutions = '[{"id":"institution-1350","name":"고용노동부 고객상담센터","description":"임금, 근로시간, 계약 관련 상담","contact":"1350"}]',
    case_status = '개인정보 제외 요약 기준으로 분석 완료',
    updated_at = current_timestamp
where id = '55555555-5555-5555-5555-555555555551';

update document_analysis_results
set summary = '근로조건 서면명시 항목이 충분한지 확인해야 하는 계약입니다. 휴게시간과 휴일 항목을 별도로 점검해야 합니다.',
    risk_flags = '[{"code":"WRITTEN_CONDITION_REVIEW","label":"근로조건 서면명시 확인 필요","level":"중간","description":"필수 근로조건이 계약서에 빠짐없이 적혀 있는지 확인해야 합니다."}]',
    issue_candidates = '["WRITTEN_WORKING_CONDITIONS","REST_BREAK","ANNUAL_LEAVE"]',
    generated_analysis = '{"status":"COMPLETED","text":"계약서에는 근로조건의 핵심 항목이 서면으로 명시되어야 합니다. 임금, 소정근로시간, 휴일, 연차유급휴가, 취업장소와 업무가 빠져 있거나 모호하다면 보완 또는 재작성 요청이 필요합니다."}',
    findings = '[{"id":"finding-written-conditions","title":"필수 근로조건 기재 여부","description":"임금, 근로시간, 휴일, 연차유급휴가 등 필수 항목이 빠짐없이 기재되어야 합니다.","severity":"중간","fieldName":"writtenConditions"},{"id":"finding-break-time","title":"휴게시간 구체성 확인","description":"휴게시간이 근로시간 중 언제 부여되는지 확인할 필요가 있습니다.","severity":"낮음","fieldName":"breakTime"}]',
    citations = '[{"id":"law-lsa-17","type":"LAW","title":"근로기준법 제17조 근로조건의 명시","sourceUrl":"https://www.law.go.kr/법령/근로기준법/제17조"},{"id":"guide-written-condition","type":"GUIDE","title":"고용노동 가이드북 근로조건 서면명시"}]',
    recommended_actions = '[{"id":"action-request-written-condition","label":"누락 항목 보완 요청","description":"필수 항목이 누락된 경우 사용자에게 계약서 보완 또는 재작성을 요청하세요.","priority":"중간","institutionName":"고용노동부 고객상담센터","expectedPath":"누락 항목 표시 -> 서면 보완 요청"}]',
    related_institutions = '[{"id":"institution-1350","name":"고용노동부 고객상담센터","description":"근로계약서 작성과 근로조건 명시 관련 상담","contact":"1350"}]',
    case_status = '개인정보 제외 요약 기준으로 분석 완료',
    updated_at = current_timestamp
where id = '55555555-5555-5555-5555-555555555552';

update document_analysis_results
set summary = '임금과 숙식비 공제 조건을 함께 확인해야 하는 계약입니다. 실제 지급액이 최저임금 기준에 미달하지 않는지 점검해야 합니다.',
    risk_flags = '[{"code":"MINIMUM_WAGE_AND_DEDUCTION","label":"최저임금·숙식비 공제 검토 필요","level":"높음","description":"임금에서 숙식비가 공제되는 경우 실제 지급액과 공제 근거를 함께 확인해야 합니다."}]',
    issue_candidates = '["MINIMUM_WAGE","DORMITORY_DEDUCTION","UNPAID_WAGES"]',
    generated_analysis = '{"status":"COMPLETED","text":"월급제라도 실제 근로시간 기준으로 시간급 환산이 필요합니다. 숙식비 공제가 있다면 공제 동의와 산정 근거, 최저임금 미달 여부를 함께 확인해야 하며 부족분이 있으면 차액 지급 요구 또는 임금체불 상담으로 이어질 수 있습니다."}',
    findings = '[{"id":"finding-minimum-wage","title":"최저임금 환산 필요","description":"월급과 실제 근로시간을 기준으로 시간급을 환산해 최저임금 이상인지 확인해야 합니다.","severity":"높음","fieldName":"basePay"},{"id":"finding-dormitory-deduction","title":"숙식비 공제 근거 확인","description":"기숙사비 등 공제 항목은 동의와 산정 근거가 필요합니다.","severity":"중간","fieldName":"dormitoryDeduction"}]',
    citations = '[{"id":"law-minimum-wage-6","type":"LAW","title":"최저임금법 제6조 최저임금의 효력","sourceUrl":"https://www.law.go.kr/법령/최저임금법/제6조"},{"id":"guide-dormitory-deduction","type":"GUIDE","title":"고용노동 가이드북 숙식제공 비용의 공제"},{"id":"case-minimum-wage-2015do676","type":"CASE","title":"최저임금 회피 목적의 소정근로시간 단축 관련 판례","caseNumber":"2015도676","court":"대법원","decidedAt":"2019.05.10"}]',
    recommended_actions = '[{"id":"action-calculate-wage-gap","label":"최저임금 차액 계산","description":"월급, 공제액, 실제 근로시간을 기준으로 시간급 환산표를 작성하세요.","priority":"높음","institutionName":"지방고용노동관서","expectedPath":"임금자료 정리 -> 차액 지급 요구 -> 미해결 시 진정"}]',
    related_institutions = '[{"id":"institution-labor-office","name":"지방고용노동관서","description":"임금체불, 최저임금, 공제 관련 진정 접수"},{"id":"institution-1350","name":"고용노동부 고객상담센터","description":"임금 산정과 상담 연결","contact":"1350"}]',
    case_status = '개인정보 제외 요약 기준으로 분석 완료',
    updated_at = current_timestamp
where id = '55555555-5555-5555-5555-555555555553';

update document_analysis_results
set summary = '휴일근로와 연차유급휴가 기준을 확인해야 하는 계약입니다. 휴일·휴가 항목이 불명확하면 서명 전 보완이 필요합니다.',
    risk_flags = '[{"code":"HOLIDAY_AND_LEAVE_REVIEW","label":"휴일·연차유급휴가 확인 필요","level":"중간","description":"휴일근로와 연차유급휴가 기준이 계약서에 명확히 적혀 있는지 확인해야 합니다."}]',
    issue_candidates = '["HOLIDAY_WORK_PAY","ANNUAL_LEAVE","WRITTEN_WORKING_CONDITIONS"]',
    generated_analysis = '{"status":"COMPLETED","text":"휴일, 휴일근로 가산수당, 연차유급휴가는 근로조건 확인에서 빠지기 쉬운 항목입니다. 계약서에 휴일과 휴가 기준이 없거나 실제 운영 방식과 다르면 서면 보완을 요청하는 것이 좋습니다."}',
    findings = '[{"id":"finding-holiday-pay","title":"휴일근로 가산 기준 확인","description":"휴일근로가 예정되거나 발생할 수 있다면 가산수당 기준을 확인해야 합니다.","severity":"중간","fieldName":"holidayWorkPay"},{"id":"finding-annual-leave","title":"연차유급휴가 명시 여부","description":"연차유급휴가 항목이 계약서나 안내문에 명확히 적혀 있는지 확인해야 합니다.","severity":"중간","fieldName":"annualLeave"}]',
    citations = '[{"id":"law-lsa-56-holiday","type":"LAW","title":"근로기준법 제56조 휴일근로 가산임금","sourceUrl":"https://www.law.go.kr/법령/근로기준법/제56조"},{"id":"law-lsa-60","type":"LAW","title":"근로기준법 제60조 연차 유급휴가","sourceUrl":"https://www.law.go.kr/법령/근로기준법/제60조"},{"id":"guide-holiday-leave","type":"GUIDE","title":"고용노동 가이드북 휴일 및 연차유급휴가"}]',
    recommended_actions = '[{"id":"action-confirm-holiday-leave","label":"휴일·연차 기준 보완 요청","description":"휴일근로와 연차유급휴가 기준을 계약서 또는 별도 서면으로 확인하세요.","priority":"중간","institutionName":"고용노동부 고객상담센터","expectedPath":"계약서 항목 확인 -> 보완 요청"}]',
    related_institutions = '[{"id":"institution-1350","name":"고용노동부 고객상담센터","description":"휴일, 휴가, 근로조건 관련 상담","contact":"1350"}]',
    case_status = '개인정보 제외 요약 기준으로 분석 완료',
    updated_at = current_timestamp
where id = '55555555-5555-5555-5555-555555555554';

update document_analysis_results
set summary = '임금 지급일과 임금명세서 교부 기준을 확인해야 하는 계약입니다. 정기 지급과 명세서 제공 흐름을 함께 관리해야 합니다.',
    risk_flags = '[{"code":"WAGE_STATEMENT_REVIEW","label":"임금명세서 교부 확인 필요","level":"낮음","description":"임금 지급 시 임금명세서가 함께 제공되는지 확인해야 합니다."}]',
    issue_candidates = '["WAGE_STATEMENT","UNPAID_WAGES"]',
    generated_analysis = '{"status":"COMPLETED","text":"계약서의 임금 지급일과 지급 방법은 이후 임금명세서, 체불 여부 확인의 기준이 됩니다. 급여 지급 시 구성항목, 공제항목, 계산방법이 포함된 임금명세서가 제공되는지 함께 확인하는 것이 좋습니다."}',
    findings = '[{"id":"finding-wage-payment-day","title":"임금 지급일 확인","description":"임금 지급일과 지급 방법이 명확하면 체불 여부 판단 기준이 됩니다.","severity":"낮음","fieldName":"wage"},{"id":"finding-wage-statement","title":"임금명세서 교부 흐름 확인","description":"급여 지급 시 임금명세서가 제공되는지 확인해야 합니다.","severity":"낮음","fieldName":"wageStatement"}]',
    citations = '[{"id":"law-lsa-48","type":"LAW","title":"근로기준법 제48조 임금대장 및 임금명세서","sourceUrl":"https://www.law.go.kr/법령/근로기준법/제48조"},{"id":"guide-wage-statement","type":"GUIDE","title":"고용노동 가이드북 임금명세서 교부"}]',
    recommended_actions = '[{"id":"action-check-wage-statement","label":"임금명세서 교부 확인","description":"급여 지급일마다 임금 구성항목, 공제항목, 계산방법이 담긴 명세서를 확인하세요.","priority":"낮음","institutionName":"고용노동부 고객상담센터","expectedPath":"급여일 확인 -> 명세서 수령 여부 점검"}]',
    related_institutions = '[{"id":"institution-1350","name":"고용노동부 고객상담센터","description":"임금명세서, 임금체불 관련 상담","contact":"1350"}]',
    case_status = '개인정보 제외 요약 기준으로 분석 완료',
    updated_at = current_timestamp
where id = '55555555-5555-5555-5555-555555555555';

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
