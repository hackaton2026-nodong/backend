# DB 명세 및 API 계약

최종 검토일: 2026-04-25

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
| `code` | `VARCHAR(64)` | NO | UNIQUE | 초대코드 |
| `expires_at` | `DATETIME(6)` | NO |  | 만료 시각 |
| `max_uses` | `INT` | NO |  | 최대 사용 횟수 |
| `used_count` | `INT` | NO |  | 현재 사용 횟수 |
| `active` | `BIT(1)` | NO |  | 활성 여부 |
| `default_role` | `VARCHAR(20)` | NO |  | 가입 시 부여할 `Role` |
| `created_at` | `DATETIME(6)` | NO |  | 생성 시각 |
| `updated_at` | `DATETIME(6)` | NO |  | 수정 시각 |

사용 가능 조건: `active = true`, `used_count < max_uses`, `expires_at >= now`

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
- `DocumentStatus = UPLOADED, STORED, HASHED, ANCHORED_ON_CHAIN, OCR_PROCESSING, OCR_COMPLETED, STRUCTURED, ANALYZED, FAILED`

현재 업로드 흐름은 파일 저장 후 해시를 계산하며, 정상적으로 끝나면 대체로 `HASHED` 상태를 반환한다.

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

현재 설계상 `documents.uploader_user_id`는 엔티티에서 스칼라 값으로만 관리하며, `01-schema.sql`에도 DB FK가 없다.

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
  "userType": "EMPLOYER",
  "countryCode": "KR",
  "languageCode": "ko",
  "inviteCode": null,
  "companyName": "Harmony Co",
  "companyBusinessNumber": "123-45-67890",
  "companyIndustry": "Manufacturing",
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
- `documents.uploader_user_id`는 `01-schema.sql`에 DB FK가 없다.
- `dashboards` 테이블은 존재하지만 현재 `/api/dashboard/*` 엔드포인트는 계산형 응답을 반환하며 이 테이블을 사용하지 않는다.
- `consultations`는 감사 시각을 저장하지만 `ConsultationResponse`는 이를 노출하지 않는다.
- `CreateDocumentRequest`는 존재하지만 현재 multipart 업로드 엔드포인트에서는 사용하지 않는다.
- `CreateDashboardRequest`와 `DashboardResponse`는 존재하지만 현재 대시보드 CRUD 컨트롤러는 노출되어 있지 않다.
- `ChecklistController`와 `ConsultationController`는 대부분의 신규 엔드포인트와 달리 `/api` 하위 경로가 아니다.
