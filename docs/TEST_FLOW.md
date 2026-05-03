# API 통합 테스트 플로우

서버: `http://localhost:8080` / 프로파일: `local`

---

## 사전 준비

### 서버 실행

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

### DB 초기화 (선택)

```bash
docker exec backend-mysql mysql -u root -p1234 backend -e "
DELETE FROM document_signatures;
DELETE FROM document_anchors;
DELETE FROM document_analysis_results;
DELETE FROM document_extractions;
DELETE FROM documents;
DELETE FROM case_checklist_statuses;
DELETE FROM consultation_messages;
DELETE FROM consultations;
DELETE FROM alerts;
DELETE FROM cases;
DELETE FROM company_invite_codes;
DELETE FROM users;
DELETE FROM enterprises;
"
```

> 초기화 후 서버를 재시작하면 `data-local.sql`의 시드 데이터가 자동으로 다시 삽입된다.

---

## 1단계 — 고용주 온보딩

### 테스트 1 — 고용주 가입 + 회사 자동 생성

```
POST /api/auth/signup
```

`inviteCode` 없이 회사 정보를 함께 전송하면 Enterprise가 자동 생성되고 해당 유저는 `ADMIN / EMPLOYER`로 등록된다.

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "employer@test.com",
    "password": "Password1!",
    "name": "TestEmployer",
    "phoneNumber": "010-1111-2222",
    "countryCode": "KR",
    "languageCode": "ko",
    "companyName": "TestCompany",
    "companyBusinessNumber": "111-11-11111",
    "companyIndustry": "manufacturing",
    "companyAddress": "Seoul Gangnam",
    "foreignWorkerQuota": 10,
    "employmentPermitCertNo": "EP-001",
    "companyCountryCode": "KR",
    "companyLanguageCode": "ko"
  }'
```

**기대 응답**

```json
{ "success": true }
```

### 테스트 2 — 로그인 (JWT 발급)

```
POST /api/auth/login
```

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{ "email": "employer@test.com", "password": "Password1!" }'
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "role": "ADMIN",
    "userType": "EMPLOYER"
  }
}
```

> 이후 모든 요청에 `Authorization: Bearer {accessToken}` 헤더 필요

### 테스트 3 — 내 프로필 조회

```
GET /api/users/me
```

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer {EMP_TOKEN}"
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "id": 17,
    "email": "employer@test.com",
    "role": "ADMIN",
    "userType": "EMPLOYER",
    "enterpriseId": 4
  }
}
```

---

## 2단계 — Case 생성 & 초대코드 발급

### 테스트 4 — Case 생성

```
POST /api/cases
```

```bash
curl -X POST http://localhost:8080/api/cases \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {EMP_TOKEN}" \
  -d '{ "industry": "manufacturing", "region": "Seoul Gangnam" }'
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "id": "90339bfd-...",
    "employerId": 17,
    "workerId": null,
    "status": "PENDING"
  }
}
```

> `workerId=null`, `status=PENDING` — 근로자 미연결 상태

### 테스트 5 — 초대코드 발급 (caseId 연결)

```
POST /enterprises/invite-codes
```

```bash
curl -X POST http://localhost:8080/enterprises/invite-codes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {EMP_TOKEN}" \
  -d '{
    "caseId": "{CASE_ID}",
    "defaultRole": "WORKER",
    "maxUses": 1,
    "expiresAt": "2027-01-01T00:00:00"
  }'
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "code": "e937dd857dfa4c16a516d08cb132e564",
    "caseId": "{CASE_ID}",
    "maxUses": 1,
    "usedCount": 0,
    "active": true,
    "defaultRole": "WORKER"
  }
}
```

> ADMIN 권한 필요. 발급된 `code`를 근로자에게 전달

---

## 3단계 — 근로자 온보딩

### 테스트 6 — 근로자 가입 (초대코드 사용)

```
POST /api/auth/signup
```

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "worker@test.com",
    "password": "Password1!",
    "name": "TestWorker",
    "phoneNumber": "010-9999-8888",
    "countryCode": "VN",
    "languageCode": "vi",
    "visaExpiresAt": "2027-12-31",
    "inviteCode": "e937dd857dfa4c16a516d08cb132e564"
  }'
```

**기대 응답**

```json
{ "success": true }
```

> 가입 즉시 Enterprise 연결, `WORKER` 역할 부여, 초대코드에 연결된 Case에 worker 자동 세팅

### 테스트 7 — Case ACTIVE 전환 확인

```
GET /api/cases/{caseId}
```

```bash
curl http://localhost:8080/api/cases/{CASE_ID} \
  -H "Authorization: Bearer {EMP_TOKEN}"
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "employerId": 17,
    "workerId": 18,
    "status": "ACTIVE"
  }
}
```

> 근로자 가입 후 `PENDING → ACTIVE` 전환, `workerId` 세팅 확인

---

## 4단계 — 문서 처리

### 문서 상태 흐름

```
UPLOADED → STORED → HASHED
                       ↓
              SIGNATURE_REQUESTED → SIGNED → ANCHOR_PENDING → ANCHORED_ON_CHAIN
                                                                ↘ ANCHOR_FAILED
                       ↓
              OCR_PROCESSING → OCR_COMPLETED → STRUCTURED → ANALYZED
                                                              ↘ FAILED
```

### DocumentType 목록

| 값 | 설명 |
|----|------|
| `EMPLOYMENT_CONTRACT` | 근로계약서. 업로드 시 근로자 초대코드 자동 발급 |
| `PAYSLIP` | 급여명세서 |
| `VISA` | 비자 |
| `RESIDENCE_PROOF` | 거주 증명 |
| `OTHER` | 기타 |

### 테스트 8 — 문서 업로드

```
POST /api/cases/{caseId}/documents
```

`multipart/form-data` 형식.

```bash
curl -X POST http://localhost:8080/api/cases/{CASE_ID}/documents \
  -H "Authorization: Bearer {WRK_TOKEN}" \
  -F "file=@/path/to/contract.pdf;type=application/pdf" \
  -F "documentType=EMPLOYMENT_CONTRACT" \
  -F "issuedAt=2026-01-01" \
  -F "expiresAt=2027-01-01"
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "id": "uuid-...",
    "caseId": "{CASE_ID}",
    "documentType": "EMPLOYMENT_CONTRACT",
    "originalFileName": "contract.pdf",
    "status": "HASHED",
    "sha256Hash": "abc123...",
    "inviteCode": {
      "code": "자동발급된코드",
      "defaultRole": "WORKER"
    }
  }
}
```

> `EMPLOYMENT_CONTRACT` 업로드 시 근로자 초대코드가 자동 발급되어 응답에 포함됨

### 테스트 9 — 문서 목록 조회

```
GET /api/cases/{caseId}/documents
```

```bash
curl http://localhost:8080/api/cases/{CASE_ID}/documents \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

### 테스트 10 — OCR 추출 요청

```
POST /api/documents/{documentId}/extraction/paddle-ocr
```

OCR worker에 비동기로 전달. 완료되면 OCR worker가 아래 콜백으로 결과를 돌려준다.

```bash
curl -X POST http://localhost:8080/api/documents/{DOC_ID}/extraction/paddle-ocr \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {WRK_TOKEN}" \
  -d '{ "ocrResult": {} }'
```

> 상태가 `OCR_PROCESSING`으로 전환됨

### 테스트 11 — OCR 결과 콜백 (내부 전용)

```
POST /api/internal/documents/{documentId}/ocr-result
```

OCR worker가 자동으로 호출하는 콜백 엔드포인트. 직접 호출 시 `X-OCR-Callback-Token` 헤더 필요.

```bash
curl -X POST http://localhost:8080/api/internal/documents/{DOC_ID}/ocr-result \
  -H "Content-Type: application/json" \
  -H "X-OCR-Callback-Token: local-ocr-token" \
  -d '{ "ocrResult": { "text": "..." } }'
```

> 상태가 `OCR_COMPLETED → STRUCTURED`로 전환됨

### 테스트 12 — OCR 추출 결과 조회

```
GET /api/documents/{documentId}/extraction
```

```bash
curl http://localhost:8080/api/documents/{DOC_ID}/extraction \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

### 테스트 13 — OCR 추출 내용 수동 보정

```
PUT /api/documents/{documentId}/extraction/correction
```

잘못 인식된 내용을 수동으로 수정.

```bash
curl -X PUT http://localhost:8080/api/documents/{DOC_ID}/extraction/correction \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {WRK_TOKEN}" \
  -d '{ "correctedText": "수정된 내용..." }'
```

### 테스트 14 — AI 문서 분석 요청

```
POST /api/documents/{documentId}/analysis
```

> `DOCUMENT_AI_ENABLED=true`일 때만 실제 분석 동작. `false`이면 stub 응답 반환.

```bash
curl -X POST http://localhost:8080/api/documents/{DOC_ID}/analysis \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

**기대 응답 구조**

```json
{
  "success": true,
  "data": {
    "status": "COMPLETED",
    "summary": "근로계약서 핵심 내용 요약...",
    "riskFlags": "위험 항목 목록...",
    "messages": [
      { "role": "USER", "content": "이 문서의 핵심 내용과 위험 요소를 분석해 주세요." },
      { "role": "ASSISTANT", "content": "요약 내용..." },
      { "role": "ASSISTANT", "content": "위험 플래그..." }
    ]
  }
}
```

### 테스트 15 — AI 분석 결과 조회

```
GET /api/documents/{documentId}/analysis
```

```bash
curl http://localhost:8080/api/documents/{DOC_ID}/analysis \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

---

## 5단계 — 서명 & 블록체인 앵커링

### 테스트 16 — 서명 요청 데이터 생성

```
GET /api/documents/{documentId}/signature-request
```

EIP-712 typed data 형식의 서명 데이터를 반환한다.

```bash
curl http://localhost:8080/api/documents/{DOC_ID}/signature-request \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "documentId": "{DOC_ID}",
    "expectedChainId": 11155111,
    "domain": { "name": "KWorkerHarmonyDocument", "version": "1", ... },
    "types": { ... },
    "message": { "documentHash": "...", "nonce": "..." },
    "typedDataHash": "0x..."
  }
}
```

> 상태가 `SIGNATURE_REQUESTED`로 전환됨. `typedDataHash`와 `nonce`를 보관해두고 서명에 사용

### 테스트 17 — 서명 제출

```
POST /api/documents/{documentId}/signatures
```

지갑으로 서명한 값을 제출한다.

```bash
curl -X POST http://localhost:8080/api/documents/{DOC_ID}/signatures \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {WRK_TOKEN}" \
  -d '{
    "walletAddress": "0xABCD...",
    "chainId": 11155111,
    "signature": "0x서명값...",
    "typedDataHash": "0x...",
    "nonce": "nonce값"
  }'
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "id": "signature-uuid",
    "status": "VERIFIED"
  }
}
```

> 상태가 `SIGNED`로 전환됨

### 테스트 18 — 블록체인 앵커링

```
POST /api/documents/{documentId}/anchor
```

서명 완료 후 문서 해시를 블록체인(Sepolia 테스트넷)에 기록.

```bash
curl -X POST http://localhost:8080/api/documents/{DOC_ID}/anchor \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {WRK_TOKEN}" \
  -d '{ "signatureId": "{SIGNATURE_ID}" }'
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "status": "PENDING",
    "txHash": "0x트랜잭션해시..."
  }
}
```

> 상태가 `ANCHOR_PENDING → ANCHORED_ON_CHAIN`으로 전환됨 (로컬에서는 StubAdapter 사용)

### 테스트 19 — 앵커링 상태 조회

```
GET /api/documents/{documentId}/anchor
```

```bash
curl http://localhost:8080/api/documents/{DOC_ID}/anchor \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

---

## 6단계 — 체크리스트

### 테스트 20 — 카탈로그 항목 조회

```
GET /checklists/items
```

```bash
curl http://localhost:8080/checklists/items \
  -H "Authorization: Bearer {EMP_TOKEN}"
```

> 전체 표준 체크리스트 항목 57개 반환. `code` 값을 아래 생성에 사용

### 테스트 21 — 체크리스트 항목 생성

```
POST /checklists
```

```bash
curl -X POST http://localhost:8080/checklists \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {EMP_TOKEN}" \
  -d '{
    "caseId": "{CASE_ID}",
    "checklistItemCode": "FEA_STANDARD_EMPLOYMENT_CONTRACT",
    "status": "IN_PROGRESS"
  }'
```

**기대 응답**

```json
{
  "success": true,
  "data": { "status": "IN_PROGRESS" }
}
```

### 테스트 22 — Case 체크리스트 전체 조회

```
GET /checklists?caseId={caseId}
```

```bash
curl "http://localhost:8080/checklists?caseId={CASE_ID}" \
  -H "Authorization: Bearer {EMP_TOKEN}"
```

> 57개 항목 전체 반환. 생성한 항목은 해당 status, 나머지는 `NOT_STARTED`

---

## 7단계 — AI 상담

### 테스트 23 — 상담 생성

```
POST /consultations
```

```bash
curl -X POST http://localhost:8080/consultations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {WRK_TOKEN}" \
  -d '{
    "diagnose": "What is the minimum wage?",
    "userId": {USER_ID}
  }'
```

**기대 응답**

```json
{
  "success": true,
  "data": { "id": 1 }
}
```

### 테스트 24 — 상담 목록 조회

```
GET /consultations
```

```bash
curl http://localhost:8080/consultations \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

---

## 8단계 — 대시보드 & 알림

### 테스트 25 — 근로자 대시보드

```
GET /api/dashboard/worker
```

```bash
curl http://localhost:8080/api/dashboard/worker \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

**응답 내 주요 필드**

| 필드 | 설명 |
|------|------|
| `header.caseId` | 현재 연결된 활성 Case ID |
| `agentCenter` | 현재 상태 기반 행동 가이드 |
| `summaryCards[risks]` | 미처리 위험 항목 수 |
| `summaryCards[checklistProgress]` | 체크리스트 진행률 |
| `summaryCards[analyzedDocuments]` | 분석 완료 문서 수 |
| `summaryCards[nextSchedule]` | 가장 가까운 문서 만료일 D-day |
| `recommendationSlot.items` | Case.region 기반 인근 교육장 추천 (최대 3개) |
| `noticePanel` | 우선순위 가장 높은 공지 |

### 테스트 26 — 고용주 대시보드

```
GET /api/dashboard/employer
```

```bash
curl http://localhost:8080/api/dashboard/employer \
  -H "Authorization: Bearer {EMP_TOKEN}"
```

**기대 응답**

```json
{
  "success": true,
  "data": {
    "activeCaseCount": 1,
    "totalChecklistCount": 0,
    "completedChecklistCount": 0,
    "unreadAlertCount": 0
  }
}
```

### 테스트 27 — 알림 목록 조회

```
GET /api/notifications
```

```bash
curl http://localhost:8080/api/notifications \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

> 현재 알림 생성 트리거 미구현으로 빈 배열 반환

### 테스트 28 — 알림 읽음 처리

```
PATCH /api/notifications/{alertId}/read
```

```bash
curl -X PATCH http://localhost:8080/api/notifications/{ALERT_ID}/read \
  -H "Authorization: Bearer {WRK_TOKEN}"
```

---

## 9단계 — 토큰 관리

### 테스트 29 — 토큰 재발급

```
POST /api/auth/reissue
```

```bash
curl -X POST http://localhost:8080/api/auth/reissue \
  -H "Content-Type: application/json" \
  -d '{ "refreshToken": "{REFRESH_TOKEN}" }'
```

### 테스트 30 — 로그아웃 + 토큰 무효화 확인

```
POST /api/auth/logout
```

```bash
# 로그아웃
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Content-Type: application/json" \
  -d '{ "accessToken": "{ACCESS_TOKEN}" }'

# 로그아웃 후 기존 토큰으로 요청 → 401 확인
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer {ACCESS_TOKEN}"
```

**기대 응답** (블랙리스트 처리됨)

```json
{ "success": false, "error": { "code": "AUTH_401" } }
```

---

## 최종 결과 요약

| # | API | 직접 테스트 | 비고 |
|---|-----|:-----------:|------|
| 1 | `POST /api/auth/signup` (고용주) | ✅ | |
| 2 | `POST /api/auth/login` | ✅ | |
| 3 | `GET /api/users/me` | ✅ | |
| 4 | `POST /api/cases` | ✅ | |
| 5 | `POST /enterprises/invite-codes` | ✅ | |
| 6 | `POST /api/auth/signup` (근로자) | ✅ | |
| 7 | `GET /api/cases/{caseId}` — ACTIVE 전환 | ✅ | |
| 8 | `POST /api/cases/{caseId}/documents` | — | multipart 파일 필요 |
| 9 | `GET /api/cases/{caseId}/documents` | — | |
| 10 | `POST /api/documents/{id}/extraction/paddle-ocr` | — | OCR worker 필요 |
| 11 | `POST /api/internal/documents/{id}/ocr-result` | — | OCR worker 콜백 |
| 12 | `GET /api/documents/{id}/extraction` | — | |
| 13 | `PUT /api/documents/{id}/extraction/correction` | — | |
| 14 | `POST /api/documents/{id}/analysis` | — | AI 엔드포인트 필요 |
| 15 | `GET /api/documents/{id}/analysis` | — | |
| 16 | `GET /api/documents/{id}/signature-request` | — | |
| 17 | `POST /api/documents/{id}/signatures` | — | 지갑 서명값 필요 |
| 18 | `POST /api/documents/{id}/anchor` | — | |
| 19 | `GET /api/documents/{id}/anchor` | — | |
| 20 | `GET /checklists/items` | ✅ | |
| 21 | `POST /checklists` | ✅ | |
| 22 | `GET /checklists?caseId=` | ✅ | |
| 23 | `POST /consultations` | ✅ | |
| 24 | `GET /consultations` | ✅ | |
| 25 | `GET /api/dashboard/worker` | ✅ | |
| 26 | `GET /api/dashboard/employer` | ✅ | |
| 27 | `GET /api/notifications` | ✅ | |
| 28 | `PATCH /api/notifications/{id}/read` | — | alert 생성 트리거 미구현 |
| 29 | `POST /api/auth/reissue` | ✅ | |
| 30 | `POST /api/auth/logout` + 무효화 확인 | ✅ | |
