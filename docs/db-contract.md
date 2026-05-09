# DB 명세 및 API 계약

최종 검토일: 2026-04-28

이 문서는 현재 프로젝트의 JPA 엔티티, DTO record, 컨트롤러, `docker/mysql/init/01-schema.sql`을 기준으로 작성했다.

## 실행 시 DB 계약

- 주 DB: Docker Compose 기반 MySQL 8.4
- 데이터베이스명: `backend`
- 문자셋/콜레이션: `utf8mb4` / `utf8mb4_unicode_ci`
- 운영/개발 DDL 모드: `spring.jpa.hibernate.ddl-auto=validate`
- 로컬 프로필 datasource: `application-local.yml`
- 테이블 생성 기준 파일: `docker/mysql/init/01-schema.sql`
- 시드 기준 파일: `docker/mysql/init/02-seed.sql`, `src/main/resources/data.sql`, `src/main/resources/data-local.sql`

현재 `src/main/resources/schema.sql`은 필수 파일이 아니다. 현 구조에서는 Docker MySQL이 `docker/mysql/init/01-schema.sql`로 스키마를 초기화하고, JPA는 엔티티와 실제 스키마가 맞는지 검증한다.

## 공통 컬럼

모든 JPA 엔티티는 `BaseEntity`를 상속하므로 저장 테이블에는 아래 컬럼이 포함된다.

| 컬럼 | 타입 | NULL | 설명 |
| --- | --- | --- | --- |
| `created_at` | `DATETIME(6)` | NO | Spring Data Auditing `@CreatedDate` |
| `updated_at` | `DATETIME(6)` | NO | Spring Data Auditing `@LastModifiedDate` |

## 테이블 명세

### `enterprises`

회사/조직 마스터 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | 회사 ID |
| `name` | `VARCHAR(100)` | NO |  | 회사명 |
| `business_number` | `VARCHAR(100)` | NO |  | 사업자 등록번호 |
| `industry` | `VARCHAR(255)` | NO |  | 업종 |
| `address` | `VARCHAR(255)` | YES |  | 사업장 주소 |
| `foreign_worker_quota` | `INT` | YES |  | 외국인 고용 허가 인원 |
| `employment_permit_cert_no` | `VARCHAR(100)` | YES |  | 고용허가제 인증번호 |
| `country_code` | `VARCHAR(10)` | NO |  | 국가 코드 |
| `language_code` | `VARCHAR(10)` | NO |  | 언어 코드 |
| `status` | `VARCHAR(20)` | NO |  | `EnterpriseStatus` |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enum: `EnterpriseStatus = ACTIVE, INACTIVE`

### `users`

애플리케이션 사용자 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | 사용자 ID |
| `email` | `VARCHAR(255)` | NO | UNIQUE | 로그인 이메일 |
| `password_hash` | `VARCHAR(255)` | NO |  | BCrypt 해시 |
| `name` | `VARCHAR(100)` | NO |  | 사용자 이름 |
| `birth_date` | `DATE` | YES |  | 근로자 생년월일 |
| `phone_number` | `VARCHAR(30)` | YES |  | 사용자 연락처 |
| `visa_expires_at` | `DATE` | YES |  | 근로자 체류 만료일 |
| `role` | `VARCHAR(20)` | NO |  | `Role` |
| `user_type` | `VARCHAR(20)` | NO |  | `UserType` |
| `status` | `VARCHAR(20)` | NO |  | `UserStatus` |
| `country_code` | `VARCHAR(10)` | NO |  | 사용자 국가 코드 |
| `language_code` | `VARCHAR(10)` | NO |  | 사용자 언어 코드 |
| `enterprise_id` | `BIGINT` | YES | FK | `enterprises.id` |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enums:
- `Role = ADMIN, EMPLOYER, WORKER`
- `UserType = WORKER, EMPLOYER`
- `UserStatus = PENDING, ACTIVE, INACTIVE`

### `company_invite_codes`

기존 회사에 합류하기 위한 초대코드 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | 초대코드 ID |
| `enterprise_id` | `BIGINT` | NO | FK | `enterprises.id` |
| `case_id` | `VARCHAR(36)` | YES |  | 근로자 온보딩 시 연결할 케이스 ID |
| `code` | `VARCHAR(64)` | NO | UNIQUE | 초대코드 |
| `expires_at` | `DATETIME(6)` | NO |  | 만료 시각 |
| `max_uses` | `INT` | NO |  | 최대 사용 횟수 |
| `used_count` | `INT` | NO |  | 현재 사용 횟수 |
| `active` | `BIT(1)` | NO |  | 활성 여부 |
| `default_role` | `VARCHAR(20)` | NO |  | 가입 시 부여할 `Role` |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

사용 가능 조건: `active = true`, `used_count < max_uses`, `expires_at >= now`

`case_id`가 있는 초대코드로 근로자가 가입하거나 `/api/companies/join`을 호출하면 해당 케이스의 `worker_id`를 가입/합류 사용자로 채우고 케이스 상태를 `ACTIVE`로 전환한다. 현재 `case_id`는 JPA/서비스에서 스칼라 값으로 관리하며 DB FK는 없다.

### `cases`

고용주, 근로자, 회사를 연결하는 작업 단위 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `employer_id` | `BIGINT` | YES | FK | `users.id` |
| `worker_id` | `BIGINT` | YES | FK | `users.id` |
| `enterprise_id` | `BIGINT` | NO | FK | `enterprises.id` |
| `status` | `VARCHAR(30)` | NO |  | `CaseStatus` |
| `industry` | `VARCHAR(100)` | NO |  | 케이스 업종 |
| `region` | `VARCHAR(100)` | NO |  | 케이스 지역 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enum: `CaseStatus = ACTIVE, PENDING, CLOSED`

### `documents`

업로드 문서의 메타데이터와 처리 상태 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `case_id` | `VARCHAR(36)` | YES | FK | `cases.id` |
| `uploader_user_id` | `BIGINT` | YES |  | 업로드 사용자 ID. 현재 DB FK 없음 |
| `document_type` | `VARCHAR(50)` | YES |  | `DocumentType` 문자열 |
| `original_file_name` | `VARCHAR(255)` | YES |  | 원본 파일명 |
| `storage_key` | `VARCHAR(255)` | YES |  | 로컬 저장 키 |
| `mime_type` | `VARCHAR(255)` | YES |  | MIME 타입 |
| `file_size` | `BIGINT` | YES |  | 파일 크기(bytes) |
| `sha256_hash` | `VARCHAR(64)` | YES |  | SHA-256 해시 |
| `anchored_tx_id` | `VARCHAR(255)` | YES |  | 외부 앵커 트랜잭션 ID |
| `status` | `VARCHAR(50)` | NO |  | `DocumentStatus` |
| `issued_at` | `DATE` | YES |  | 발급일 |
| `expires_at` | `DATE` | YES |  | 만료일 |
| `ocr_completed_at` | `DATETIME(6)` | YES |  | OCR 완료 시각 |
| `analyzed_at` | `DATETIME(6)` | YES |  | 분석 완료 시각 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enums:
- `DocumentType = EMPLOYMENT_CONTRACT, PAYSLIP, VISA, RESIDENCE_PROOF, OTHER`
- `DocumentStatus = UPLOADED, STORED, HASHED, SIGNATURE_REQUESTED, SIGNED, ANCHOR_PENDING, ANCHORED_ON_CHAIN, ANCHOR_FAILED, OCR_PROCESSING, OCR_COMPLETED, STRUCTURED, ANALYZED, FAILED`

현재 업로드 흐름은 파일 저장 후 해시를 계산하며, 정상적으로 끝나면 대체로 `HASHED` 상태를 반환한다.

### `document_signatures`

문서 해시 기반 EIP-712 서명 요청과 제출된 지갑 서명을 저장한다.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `document_id` | `VARCHAR(36)` | NO | FK | `documents.id` |
| `user_id` | `BIGINT` | NO | FK | 서명 사용자 |
| `wallet_address` | `VARCHAR(42)` | YES | UNIQUE pair | 제출 지갑 주소 |
| `chain_id` | `BIGINT` | NO | UNIQUE pair | 체인 ID |
| `verifying_contract` | `VARCHAR(42)` | NO | UNIQUE pair | EIP-712 verifying contract |
| `typed_data_hash` | `VARCHAR(66)` | NO | UNIQUE | 서버 재계산 typed data hash |
| `client_typed_data_hash` | `VARCHAR(66)` | YES |  | 클라이언트 제출 참고값 |
| `signature` | `TEXT` | YES |  | 지갑 서명값 |
| `signature_hash` | `VARCHAR(66)` | YES | UNIQUE | 서명값 해시 |
| `nonce` | `VARCHAR(66)` | NO | UNIQUE pair | 서명 nonce |
| `deadline` | `DATETIME(6)` | NO |  | 서명 만료 시각 |
| `status` | `VARCHAR(50)` | NO |  | `DocumentSignatureStatus` |
| `signed_at` | `DATETIME(6)` | YES |  | 서명 저장 시각 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Unique 제약: `(document_id, user_id, wallet_address)`, `(chain_id, verifying_contract, nonce)`, `(typed_data_hash)`, `(signature_hash)`

Enum: `DocumentSignatureStatus = REQUESTED, SIGNED, EXPIRED, REJECTED`

### `document_anchors`

서버 relayer 앵커링 요청과 결과를 저장한다.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `document_id` | `VARCHAR(36)` | NO | FK | `documents.id` |
| `signature_id` | `VARCHAR(36)` | NO | FK | `document_signatures.id` |
| `chain_id` | `BIGINT` | NO | UNIQUE pair | 체인 ID |
| `contract_address` | `VARCHAR(42)` | NO | UNIQUE pair | 컨트랙트 주소 |
| `anchor_id` | `VARCHAR(66)` | NO | UNIQUE pair | 앵커 ID |
| `document_hash` | `VARCHAR(66)` | NO |  | 문서 해시 |
| `case_id_hash` | `VARCHAR(66)` | NO |  | 케이스 ID 해시 |
| `tx_hash` | `VARCHAR(66)` | YES | UNIQUE | 트랜잭션 해시 |
| `block_number` | `BIGINT` | YES |  | 블록 번호 |
| `status` | `VARCHAR(50)` | NO |  | `DocumentAnchorStatus` |
| `retry_count` | `INT` | NO |  | 재시도 횟수 |
| `last_error_message` | `VARCHAR(1000)` | YES |  | 마지막 실패 메시지 |
| `anchored_at` | `DATETIME(6)` | YES |  | 앵커 완료 시각 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Unique 제약: `(chain_id, contract_address, anchor_id)`, `(tx_hash)`

Enum: `DocumentAnchorStatus = PENDING, ANCHORED, FAILED`

### `document_analysis_results`

오프체인 FastAPI 분석 결과와 해시를 저장한다. 실제 OCR 추출 결과는 `document_extractions`에 저장하며, extraction payload와 AI 레이어 사이의 payload 규격은 [offchain-analysis-contract.md](offchain-analysis-contract.md)를 따른다. AI 비활성/미설정 또는 upstream 실패는 `FAILED` 상태로 기록한다.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `document_id` | `VARCHAR(36)` | NO | UNIQUE, FK | `documents.id` |
| `status` | `VARCHAR(50)` | NO |  | `DocumentAnalysisStatus` |
| `extracted_text_hash` | `VARCHAR(64)` | YES |  | 마스킹/정규화된 AI request 또는 normalized terms 해시 |
| `analysis_result_hash` | `VARCHAR(64)` | YES |  | 분석 결과 해시 |
| `summary` | `TEXT` | YES |  | 요약 |
| `risk_flags` | `TEXT` | YES |  | 위험 플래그 JSON 문자열 |
| `analyzed_at` | `DATETIME(6)` | YES |  | 분석 완료 시각 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enum: `DocumentAnalysisStatus = PENDING, COMPLETED, FAILED`

필터링 전 raw OCR text 저장 테이블은 만들지 않는다. 원문 파일, `storageKey`, 필터링 전 OCR 전문은 AI request와 로그에 포함하지 않는다.

### `document_extractions`

OCR 결과에서 근로계약서 분석에 필요한 필드만 추출해 저장하는 테이블이다. PaddleOCR 원본 JSON, raw markdown, raw table HTML, raw OCR 전문은 저장하지 않는다.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `document_id` | `VARCHAR(36)` | NO | UNIQUE, FK | `documents.id` |
| `status` | `VARCHAR(50)` | NO |  | `DocumentExtractionStatus` |
| `schema_version` | `VARCHAR(100)` | NO |  | 추출 payload 스키마 버전 |
| `source_engine` | `VARCHAR(100)` | NO |  | OCR/parser 엔진명. 예: `PADDLE_OCR` |
| `source_result_hash` | `VARCHAR(64)` | YES |  | 저장하지 않는 원본 OCR JSON의 canonical hash |
| `extracted_payload` | `TEXT` | YES |  | 민감정보 제거 후 구조화한 계약 필드 JSON |
| `corrected_payload` | `TEXT` | YES |  | 사용자 보정 후 최종 계약 필드 JSON |
| `ai_payload_hash` | `VARCHAR(64)` | YES |  | AI 전달 기준 payload hash |
| `review_required_reason` | `VARCHAR(1000)` | YES |  | 보정 필요 사유 |
| `extracted_at` | `DATETIME(6)` | YES |  | 추출 완료 시각 |
| `corrected_at` | `DATETIME(6)` | YES |  | 보정 완료 시각 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enum: `DocumentExtractionStatus = PENDING, EXTRACTED, NEEDS_REVIEW, CORRECTED, FAILED`

AI 요청은 `corrected_payload`가 있으면 이를 우선 사용하고, 없으면 `extracted_payload`를 사용한다. 두 payload 모두 이름, 전화번호, 이메일, 사업자등록번호, 상세주소, 본국 주소, 원문 OCR 전문, `storageKey`를 포함하면 안 된다.

### `alerts`

사용자 알림 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `user_id` | `BIGINT` | NO | FK | `users.id` |
| `title` | `VARCHAR(100)` | NO |  | 알림 제목 |
| `message` | `VARCHAR(1000)` | NO |  | 알림 내용 |
| `type` | `VARCHAR(30)` | NO |  | `AlertType` |
| `is_read` | `BIT(1)` | NO |  | 읽음 여부 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Enum: `AlertType = GENERAL, CHECKLIST, CASE, SYSTEM`

### `dashboards`

기존 대시보드 요약 엔티티 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `user_id` | `BIGINT` | NO | FK | `users.id` |
| `title` | `VARCHAR(100)` | NO |  | 대시보드 제목 |
| `summary` | `VARCHAR(1000)` | NO |  | 대시보드 요약 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

현재 활성 대시보드 API는 `cases`, `documents`, `case_checklist_statuses`, `alerts`를 기반으로 역할별 응답을 계산한다. 이 테이블은 현재 `/api/dashboard/*` 응답 생성에 사용되지 않는다.

### `case_checklist_statuses`

케이스별 체크리스트 항목 상태 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `VARCHAR(36)` | NO | PK | UUID 문자열 |
| `case_id` | `VARCHAR(36)` | NO | FK, UNIQUE pair | `cases.id` |
| `checklist_item_code` | `VARCHAR(100)` | NO | UNIQUE pair | 체크리스트 카탈로그 항목 코드 |
| `status` | `VARCHAR(20)` | NO |  | `ChecklistStatus` |
| `note` | `VARCHAR(1000)` | YES |  | 상태 메모 |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

Unique 제약: `(case_id, checklist_item_code)`

Enum: `ChecklistStatus = NOT_STARTED, IN_PROGRESS, COMPLETED, REVIEW_REQUIRED`

체크리스트 정의는 DB에 저장하지 않는다. `src/main/resources/reference/checklists/moel-foreign-worker-employment-management.json`에서 로드한다.

### `consultations`

상담/진단 문자열 저장 테이블.

| 컬럼 | 타입 | NULL | 키 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | `BIGINT AUTO_INCREMENT` | NO | PK | 상담 ID |
| `diagnose` | `VARCHAR(1000)` | NO |  | 진단/상담 텍스트 |
| `uid` | `BIGINT` | NO | FK | `users.id` |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

## 관계

- `users.enterprise_id -> enterprises.id`
- `company_invite_codes.enterprise_id -> enterprises.id`
- `cases.enterprise_id -> enterprises.id`
- `cases.employer_id -> users.id`
- `cases.worker_id -> users.id`
- `documents.case_id -> cases.id`
- `alerts.user_id -> users.id`
- `dashboards.user_id -> users.id`
- `case_checklist_statuses.case_id -> cases.id`
- `consultations.uid -> users.id`

현재 설계상 `company_invite_codes.case_id`와 `documents.uploader_user_id`는 엔티티에서 스칼라 값으로만 관리하며, `01-schema.sql`에도 DB FK가 없다.

## 로컬 시드 시나리오

`src/main/resources/data-local.sql`과 `docker/mysql/init/02-seed.sql`은 DB align 검증용으로 아래 시나리오를 생성한다.

- 사업장 1개: `한국제조`
- 고용주 1명: `minsukim@hankukmanufacturing.co.kr` / `password123`
- 근로자 5명: `minh.nguyen97@example.com`, `somchai.phanit95@example.com`, `maria.santos98@example.com`, `dewi.lestari96@example.com`, `ram.thapa94@example.com` / `password123`
- 활성 케이스 5개: 각 근로자별 1개
- 근로계약서 문서 5개: 각 케이스별 `EMPLOYMENT_CONTRACT`, `ANALYZED` 상태
- 분석 결과 5개: placeholder summary/risk flags
- 케이스 연결 초대코드 5개: `KOHAMO-WORKER-1` ~ `KOHAMO-WORKER-5`

이 시드는 실제 온체인 호출, 실제 전자서명, 실제 AI 분석을 수행하지 않는다. 화면/DB 플로우 검증을 위해 문서와 분석 상태만 미리 채운다.

## API 공통 응답

일반 JSON API는 아래 envelope 형태를 반환한다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

성공 응답에서 `data`와 `error`가 null이면 직렬화 결과에서 생략된다.

에러 응답 형태:

```json
{
  "success": false,
  "error": {
    "code": "COMMON_400",
    "message": "Invalid input value"
  }
}
```

에러 코드:

| HTTP | 코드 | 의미 |
| --- | --- | --- |
| 400 | `COMMON_400` | 잘못된 입력값 |
| 401 | `AUTH_401` | 인증 필요 |
| 401 | `AUTH_401_1` | 이메일 또는 비밀번호 오류 |
| 401 | `AUTH_401_2` | 유효하지 않은 토큰 |
| 401 | `AUTH_401_3` | 리프레시 토큰 없음 |
| 401 | `AUTH_401_4` | 토큰 타입 불일치 |
| 403 | `AUTH_403` | 접근 거부 |
| 404 | `COMMON_404` | 리소스 없음 |
| 409 | `COMMON_409` | 리소스 중복 |
| 500 | `COMMON_500` | 서버 내부 오류 |

인증 없이 접근 가능한 경로:
- `/auth/**`
- `/api/auth/**`
- `/document-upload-test.html`
- `/dashboard-api-preview.html`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

그 외 엔드포인트는 `Authorization: Bearer <accessToken>` 헤더를 기대한다.

## API 계약

### Auth

기본 경로: `/auth`, `/api/auth`

#### `POST /signup`

요청:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "User Name",
  "birthDate": null,
  "phoneNumber": "010-1000-0000",
  "visaExpiresAt": null,
  "userType": "EMPLOYER",
  "countryCode": "KR",
  "languageCode": "ko",
  "inviteCode": null,
  "companyName": "Harmony Co",
  "companyBusinessNumber": "123-45-67890",
  "companyIndustry": "Manufacturing",
  "companyAddress": "Seoul",
  "foreignWorkerQuota": 5,
  "employmentPermitCertNo": "EPS-001",
  "companyCountryCode": "KR",
  "companyLanguageCode": "ko"
}
```

검증:
- `email`: 필수, 이메일 형식
- `password`: 필수, 길이 8-100
- `name`: 필수, 최대 100자
- `countryCode`, `languageCode`: 필수
- `inviteCode` 또는 전체 회사 필드 묶음 중 정확히 하나만 제공해야 함
- 고용주 가입은 회사 필드 묶음으로 `Enterprise`를 생성하고 사용자와 연결함
- 근로자 가입은 `inviteCode`로 기존 회사에 연결하며, 초대코드에 `caseId`가 있으면 해당 케이스도 연결함
- `birthDate`, `phoneNumber`, `visaExpiresAt`, 사업장 추가 필드는 nullable이지만 온보딩 UI 입력값 저장용으로 사용함

응답 데이터: 없음

#### `POST /login`

요청:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

응답 데이터:

```json
{
  "accessToken": "jwt",
  "refreshToken": "jwt"
}
```

#### `POST /reissue`

요청:

```json
{
  "refreshToken": "jwt"
}
```

응답 데이터:

```json
{
  "accessToken": "jwt"
}
```

#### `POST /logout`

요청:

```json
{
  "accessToken": "jwt"
}
```

응답 데이터: 없음

### Companies / Enterprises

기본 경로: `/enterprises`, `/api/companies`

#### `GET /`

응답 데이터: `EnterpriseResponse[]`

```json
[
  {
    "id": 1,
    "name": "Harmony Co",
    "businessNumber": "123-45-67890",
    "industry": "Manufacturing",
    "address": "Seoul",
    "foreignWorkerQuota": 5,
    "employmentPermitCertNo": "EPS-001",
    "countryCode": "KR",
    "languageCode": "ko",
    "status": "ACTIVE"
  }
]
```

#### `GET /{enterpriseId}`

응답 데이터: `EnterpriseResponse`

#### `POST /`

요청:

```json
{
  "name": "Harmony Co",
  "businessNumber": "123-45-67890",
  "industry": "Manufacturing",
  "address": "Seoul",
  "foreignWorkerQuota": 5,
  "employmentPermitCertNo": "EPS-001",
  "countryCode": "KR",
  "languageCode": "ko"
}
```

응답 데이터: `EnterpriseResponse`

#### `POST /invite-codes`

인증 필요.

요청:

```json
{
  "caseId": "case-uuid",
  "expiresAt": "2026-12-31T23:59:59",
  "maxUses": 10,
  "defaultRole": "WORKER"
}
```

응답 데이터:

```json
{
  "id": 1,
  "companyId": 1,
  "caseId": "case-uuid",
  "code": "INVITE-CODE",
  "expiresAt": "2026-12-31T23:59:59",
  "maxUses": 10,
  "usedCount": 0,
  "active": true,
  "defaultRole": "WORKER"
}
```

#### `POST /join`

인증 필요.

요청:

```json
{
  "inviteCode": "INVITE-CODE"
}
```

응답 데이터: `EnterpriseResponse`

#### `GET /users`

인증 필요.

응답 데이터:

```json
[
  {
    "id": 1,
    "email": "worker@example.com",
    "name": "Worker",
    "phoneNumber": "010-2000-0001",
    "role": "WORKER",
    "status": "ACTIVE"
  }
]
```

### Cases

기본 경로: `/api/cases`

#### `GET /active`

인증 필요. 현재 사용자가 조회 가능한 활성 케이스 목록을 반환한다.

응답 데이터: `CaseResponse[]`

```json
[
  {
    "id": "uuid",
    "companyId": 1,
    "employerId": 2,
    "workerId": 3,
    "status": "ACTIVE",
    "industry": "Manufacturing",
    "region": "Seoul",
    "createdAt": "2026-04-25T12:00:00",
    "updatedAt": "2026-04-25T12:00:00"
  }
]
```

#### `GET /{caseId}`

인증 필요. 응답 데이터: `CaseResponse`

#### `POST /`

인증 필요.

요청:

```json
{
  "industry": "Manufacturing",
  "region": "Seoul"
}
```

응답 데이터: `CaseResponse`

#### `POST /{caseId}/members`

인증 필요.

요청:

```json
{
  "employerId": 2,
  "workerId": 3
}
```

응답 데이터: `CaseResponse`

### Documents

기본 경로: `/api`

#### `GET /cases/{caseId}/documents`

인증 필요.

응답 데이터: `DocumentResponse[]`

```json
[
  {
    "id": "uuid",
    "caseId": "case-uuid",
    "uploaderUserId": 3,
    "documentType": "EMPLOYMENT_CONTRACT",
    "originalFileName": "contract.pdf",
    "storageKey": "documents/...",
    "stored": true,
    "mimeType": "application/pdf",
    "fileSize": 12345,
    "sha256Hash": "hex",
    "anchoredTxId": null,
    "status": "HASHED",
    "issuedAt": "2026-01-01",
    "expiresAt": "2027-01-01",
    "ocrCompletedAt": null,
    "analyzedAt": null
  }
]
```

#### `GET /documents/{documentId}`

인증 필요. 응답 데이터: `DocumentResponse`

#### `POST /cases/{caseId}/documents`

인증 필요. `multipart/form-data` 요청.

파트/파라미터:
- `file`: multipart file part
- `documentType`: `DocumentType` 중 하나
- `issuedAt`: ISO 날짜. 예: `2026-01-01`
- `expiresAt`: ISO 날짜. 예: `2027-01-01`

응답 데이터: `DocumentResponse`

#### `GET /documents/{documentId}/signature-request`

인증 필요. 해시 완료 문서에 대해 EIP-712 서명 payload를 생성한다.

응답 데이터: `DocumentSignatureRequestResponse`

```json
{
  "documentId": "document-uuid",
  "expectedChainId": 11155111,
  "domain": {
    "name": "KWorkerHarmonyDocument",
    "version": "1",
    "chainId": 11155111,
    "verifyingContract": "0x0000000000000000000000000000000000000000"
  },
  "types": {
    "DocumentConsent": [
      {"name": "documentId", "type": "string"},
      {"name": "caseId", "type": "string"},
      {"name": "documentHash", "type": "bytes32"},
      {"name": "documentType", "type": "string"},
      {"name": "signerUserId", "type": "uint256"},
      {"name": "nonce", "type": "bytes32"},
      {"name": "deadline", "type": "uint256"}
    ]
  },
  "message": {
    "documentId": "document-uuid",
    "caseId": "case-uuid",
    "documentHash": "0x...",
    "documentType": "EMPLOYMENT_CONTRACT",
    "signerUserId": 2,
    "nonce": "0x...",
    "deadline": 1770000000
  },
  "typedDataHash": "0x..."
}
```

현재 MVP는 서버 payload 재계산과 요청 일관성 검증을 제공하며, 실제 ECDSA signer recovery는 후속 작업이다.

#### `POST /documents/{documentId}/signatures`

인증 필요. MetaMask 등 EVM 지갑에서 받은 EIP-712 서명을 제출한다.

요청:

```json
{
  "walletAddress": "0x1111111111111111111111111111111111111111",
  "chainId": 11155111,
  "signature": "0x...",
  "typedDataHash": "0x...",
  "nonce": "0x..."
}
```

응답 데이터: `DocumentSignatureResponse`

```json
{
  "signatureId": "signature-uuid",
  "documentId": "document-uuid",
  "walletAddress": "0x1111111111111111111111111111111111111111",
  "status": "SIGNED",
  "signedAt": "2026-04-26T15:00:00"
}
```

#### `POST /documents/{documentId}/anchor`

인증 필요. 저장된 서명을 기준으로 Stub relayer 앵커링을 수행한다.

요청:

```json
{
  "signatureId": "signature-uuid"
}
```

응답 데이터: `DocumentAnchorResponse`

```json
{
  "anchorId": "0x...",
  "documentId": "document-uuid",
  "status": "ANCHORED_ON_CHAIN",
  "contractAddress": "0x0000000000000000000000000000000000000000",
  "chainId": 11155111,
  "txHash": "0x...",
  "blockNumber": 1,
  "anchoredAt": "2026-04-26T15:05:00"
}
```

현재 MVP는 실제 Sepolia RPC 전송 대신 Stub tx hash와 block number를 저장한다.

#### `GET /documents/{documentId}/anchor`

인증 필요. 가장 최근 앵커링 결과를 조회한다.

응답 데이터: `DocumentAnchorResponse`

#### `POST /documents/{documentId}/analysis`

인증 필요. 오프체인 분석 결과를 생성 또는 갱신한다. `document_extractions.corrected_payload`가 있으면 이를 우선 사용하고, 없으면 `extracted_payload`를 사용해 sanitized AI request를 구성한다. `DOCUMENT_AI_ENABLED=true`이면 `DOCUMENT_AI_ENDPOINT`로 POST하고, 비활성 또는 endpoint 미설정 상태에서는 placeholder 완료 결과를 저장하지 않고 `FAILED` 분석을 남긴 뒤 HTTP 503을 반환한다. Spring은 FastAPI 요청의 `outputRequest.includeGeneratedAnalysis`를 `true`로 설정한다.

응답 데이터: `DocumentAnalysisResponse`

```json
{
  "id": "analysis-uuid",
  "documentId": "document-uuid",
  "status": "COMPLETED",
  "extractedTextHash": "sha256",
  "analysisResultHash": "sha256",
  "summary": "근로계약서의 주요 근로조건은 확인되지만 추가 검토가 필요한 항목이 있습니다.",
  "riskFlags": [{ "code": "MINIMUM_WAGE_REVIEW_REQUIRED", "severity": "HIGH" }],
  "generatedAnalysis": { "status": "COMPLETED", "text": "사용자에게 표시할 자연어 분석 문장" },
  "analyzedAt": "2026-04-26T15:06:00"
}
```

#### `GET /documents/{documentId}/analysis`

인증 필요. 저장된 분석 결과를 조회한다.

응답 데이터: `DocumentAnalysisResponse`

#### `POST /documents/{documentId}/extraction/paddle-ocr`

인증 필요. 개발/수동 검증용 API다. PaddleOCR 원본 JSON을 받아 근로계약서 분석에 필요한 필드만 추출한다. 제품 플로우에서는 업로드 후 OCR worker가 내부 callback API로 결과를 제출한다.

요청:

```json
{
  "ocrResult": {
    "layoutParsingResults": []
  }
}
```

응답 데이터: `DocumentExtractionResponse`

#### `POST /internal/documents/{documentId}/ocr-result`

OCR worker callback API다. JWT 인증 대신 `X-OCR-Callback-Token` 헤더를 `DOCUMENT_OCR_CALLBACK_TOKEN`과 비교한다. 토큰 설정값이 비어 있으면 로컬 개발 편의를 위해 토큰 검증을 생략한다.

요청:

```json
{
  "ocrResult": {
    "layoutParsingResults": []
  }
}
```

응답 데이터: `DocumentExtractionResponse`

#### `GET /documents/{documentId}/extraction`

인증 필요. 저장된 추출/보정 payload를 조회한다. 응답에는 정제된 JSON만 포함되며 raw OCR 결과는 포함되지 않는다.

응답 데이터: `DocumentExtractionResponse`

#### `PUT /documents/{documentId}/extraction/correction`

인증 필요. 사용자가 보정한 최종 계약 필드 JSON을 저장한다. raw OCR 필드명, 이메일, 전화번호, 사업자등록번호 등 민감 식별자가 포함되면 거부한다.

요청:

```json
{
  "correctedPayload": {
    "schemaVersion": "employment-contract-v1",
    "contractTerms": {}
  }
}
```

응답 데이터: `DocumentExtractionResponse`

### Checklists

기본 경로: `/checklists`

#### `GET /?caseId={caseId}`

응답 데이터: `ChecklistResponse[]`

각 응답은 체크리스트 카탈로그 정의와 DB 상태를 합쳐서 반환한다. 특정 항목의 DB row가 없으면 `id`, `note`, `createdAt`, `updatedAt`은 null이고 `status`는 `NOT_STARTED`다.

```json
[
  {
    "id": "uuid",
    "caseId": "case-uuid",
    "checklistItemCode": "FEA_STANDARD_EMPLOYMENT_CONTRACT",
    "sectionCode": "CONTRACT",
    "sectionTitle": "Contract",
    "code": "FEA_STANDARD_EMPLOYMENT_CONTRACT",
    "title": "Standard employment contract",
    "description": "Checklist description",
    "required": true,
    "status": "COMPLETED",
    "note": "done",
    "createdAt": "2026-04-25T12:00:00",
    "updatedAt": "2026-04-25T12:00:00"
  }
]
```

#### `GET /{checklistId}`

응답 데이터: `ChecklistResponse`

#### `POST /`

요청:

```json
{
  "caseId": "case-uuid",
  "checklistItemCode": "FEA_STANDARD_EMPLOYMENT_CONTRACT",
  "status": "COMPLETED",
  "note": "done"
}
```

응답 데이터: `ChecklistResponse`

#### `GET /items`

응답 데이터: `ChecklistItemResponse[]`

```json
[
  {
    "sectionCode": "CONTRACT",
    "sectionTitle": "Contract",
    "code": "FEA_STANDARD_EMPLOYMENT_CONTRACT",
    "title": "Standard employment contract",
    "description": "Checklist description",
    "required": true,
    "displayOrder": 1
  }
]
```

### Alerts / Notifications

기본 경로: `/api/notifications`

#### `GET /`

인증 필요.

응답 데이터:

```json
[
  {
    "id": "uuid",
    "userId": 3,
    "title": "Checklist review pending",
    "message": "Please review checklist",
    "type": "CHECKLIST",
    "isRead": false,
    "createdAt": "2026-04-25T12:00:00",
    "updatedAt": "2026-04-25T12:00:00"
  }
]
```

#### `PATCH /{alertId}/read`

인증 필요. 알림을 읽음 처리한다.

응답 데이터: 없음

### Dashboards

기본 경로: `/api/dashboard`

#### `GET /worker`

인증 필요. 현재 사용자 타입이 `WORKER`여야 한다.

응답 데이터:

```json
{
  "header": {
    "userId": 3,
    "userName": "Worker",
    "userType": "WORKER",
    "baseDate": "2026-04-25",
    "caseId": "case-uuid"
  },
  "agentCenter": {
    "title": "체크리스트 검토가 필요해요",
    "description": "문서 분석 결과와 연결된 점검 항목에 검토가 필요한 항목이 있습니다.",
    "actions": [
      {
        "label": "체크리스트 이동",
        "targetPath": "/checklists?caseId=case-uuid"
      }
    ],
    "reasonTypes": ["CHECKLIST", "DOCUMENT"]
  },
  "summaryCards": [
    {
      "key": "risks",
      "title": "미처리 위험 항목",
      "value": "1",
      "subtitle": "즉시 확인 필요",
      "severity": "high"
    }
  ],
  "todayActions": [
    {
      "type": "CHECKLIST",
      "title": "체크리스트 점검",
      "description": "공식 체크리스트의 미완료 또는 검토 필요 항목을 확인해 주세요.",
      "ctaLabel": "문항 보기",
      "targetPath": "/checklists?caseId=case-uuid",
      "priority": "HIGH"
    }
  ],
  "recommendationSlot": {
    "title": "추천 기관 · 교육",
    "placeholderMessage": "기관/교육 추천 도메인 연결 전까지는 현재 케이스 상태를 기반으로 추천 슬롯만 제공합니다.",
    "reasonTags": ["region:Seoul", "language:ko", "industry:Manufacturing"],
    "items": []
  },
  "noticePanel": {
    "title": "주의 사항",
    "message": "만료 일정이 30일 이내로 다가온 문서가 있습니다.",
    "severity": "high",
    "ctaLabel": "관련 문서 보기",
    "targetPath": "/cases/case-uuid/documents"
  }
}
```

#### `GET /employer`

인증 필요. 현재 사용자 타입이 `EMPLOYER`여야 한다.

응답 데이터:

```json
{
  "userId": 2,
  "userType": "EMPLOYER",
  "activeCaseCount": 4,
  "totalChecklistCount": 18,
  "completedChecklistCount": 11,
  "unreadAlertCount": 2
}
```

### Consultations

기본 경로: `/consultations`

#### `GET /`

응답 데이터:

```json
[
  {
    "id": 1,
    "diagnose": "diagnosis text",
    "userId": 3
  }
]
```

#### `GET /{consultationId}`

응답 데이터: `ConsultationResponse`

#### `POST /`

요청:

```json
{
  "diagnose": "diagnosis text",
  "userId": 3
}
```

응답 데이터: `ConsultationResponse`

### Test

#### `GET /api/test/hello`

응답 본문은 envelope 없는 문자열이다.

```text
Hello, K-Worker Harmony!
```

## 현재 계약상 주의 사항

- `documents.case_id`에는 DB FK가 있지만, `Document` 엔티티는 JPA 관계가 아니라 `String` 스칼라 값으로 매핑한다.
- `company_invite_codes.case_id`는 DB FK 없이 `String` 스칼라 값으로 매핑한다.
- `documents.uploader_user_id`는 `01-schema.sql`에 DB FK가 없다.
- `dashboards` 테이블은 존재하지만 현재 `/api/dashboard/*` 엔드포인트는 계산형 응답을 반환하며 이 테이블을 사용하지 않는다.
- `consultations`는 감사 시각을 저장하지만 `ConsultationResponse`는 이를 노출하지 않는다.
- `CreateDocumentRequest`는 존재하지만 현재 multipart 업로드 엔드포인트에서는 사용하지 않는다.
- `CreateDashboardRequest`와 `DashboardResponse`는 존재하지만 현재 대시보드 CRUD 컨트롤러는 노출되어 있지 않다.
- `ChecklistController`와 `ConsultationController`는 대부분의 신규 엔드포인트와 달리 `/api` 하위 경로가 아니다.
