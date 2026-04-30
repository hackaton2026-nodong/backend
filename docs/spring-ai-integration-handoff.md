# Spring AI Integration Handoff

Last updated: 2026-04-30

이 문서는 Spring 백엔드 작업자가 AI 팀과 병렬로 진행할 수 있도록, Spring 쪽 책임 범위와 작업 단위를 정리한 핸드오프 문서다.

## 한 줄 결론

OCR은 Spring 파이프라인에서 처리한다. FastAPI는 원본 파일이나 원문 OCR 텍스트를 받지 않고, Spring이 저장한 sanitized extraction payload를 받아서 Graph RAG 기반 분석 결과를 반환한다.

```text
파일 업로드
-> Spring 원문 저장/해시
-> OCR worker
-> Spring sanitizer/extractor
-> document_extractions 저장
-> Spring -> FastAPI /document-analysis
-> Spring document_analysis_results 저장
-> 프론트 조회
```

## 현재 상태

이미 동작하는 것:

- 문서 업로드
- 원문 파일 저장
- SHA-256 hash 생성
- OCR worker 요청
- PaddleOCR callback 수신
- 근로계약서 OCR 결과 구조화
- `document_extractions.extracted_payload` 저장
- `POST /api/documents/{documentId}/analysis` 엔드포인트
- AI 비활성 시 placeholder 분석 결과 저장

아직 해야 하는 것:

- 실제 FastAPI `/document-analysis` 호출 활성화
- AI 응답 schema 확정 및 테스트 fixture 고정
- 상세 분석 결과 저장 구조 확장
- 프론트에서 분석 결과를 표시하기 쉬운 조회 응답 확장
- 분석 트리거 정책 결정
- 운영용 timeout/retry/error handling 보강
- 이후 챗봇 streaming 연결 방식 결정

현재 지원 범위:

- `EMPLOYMENT_CONTRACT` 우선
- `PAYSLIP`은 추후 단계

## 역할 분리

### Spring 책임

- 사용자 인증/인가
- 케이스/문서 소유권 검증
- 파일 업로드
- 원문 파일 저장
- document hash 생성
- 온체인 서명/앵커링
- OCR worker 호출
- OCR callback 수신
- sanitized extraction payload 생성
- AI request 생성
- AI response 검증
- 분석 결과 DB 저장
- 프론트용 분석 조회 API 제공

### FastAPI 책임

- Spring이 보낸 sanitized payload 검증
- 근로계약서 rule check
- Issue 후보 생성
- Neo4j Graph RAG 조회
- OpenAI 기반 분석 생성
- 법령/가이드/판례/기관/해결방법 근거 포함
- Spring이 저장할 수 있는 JSON response 반환

### Spring이 하지 않는 것

- FastAPI에 원문 파일 전달
- FastAPI에 raw OCR 전문 전달
- FastAPI에서 OCR 수행
- Neo4j에 사용자 문서 저장
- 사용자 개인정보를 AI/RAG 지식 그래프에 저장

## 환경 변수

AI 연동 시 Spring 환경 변수:

```env
DOCUMENT_AI_ENABLED=true
DOCUMENT_AI_ENDPOINT=http://localhost:8000/document-analysis
```

Docker Compose 내부 통신이면 endpoint는 네트워크 이름 기준으로 바뀐다.

```env
DOCUMENT_AI_ENDPOINT=http://ai:8000/document-analysis
```

현재 기본값은 AI 비활성이다.

```yaml
document:
  ai:
    enabled: ${DOCUMENT_AI_ENABLED:false}
    endpoint: ${DOCUMENT_AI_ENDPOINT:}
```

## 현재 Spring -> FastAPI Request

현재 구현 기준으로 Spring은 아래 형태의 JSON을 FastAPI로 보낸다.

```json
{
  "requestId": "analysis-request-hash",
  "documentId": "document-uuid",
  "caseId": "case-uuid",
  "documentHash": "sha256-hex",
  "documentType": "EMPLOYMENT_CONTRACT",
  "extractionId": "extraction-uuid",
  "extractionStatus": "EXTRACTED",
  "schemaVersion": "employment-contract-v1",
  "sourceEngine": "PADDLE_OCR",
  "sourceResultHash": "sha256-hex",
  "aiPayloadHash": "sha256-hex",
  "payload": {
    "schemaVersion": "employment-contract-v1",
    "contractTerms": {
      "document": {
        "documentForm": "STANDARD_LABOR_CONTRACT",
        "standardContractUsed": true
      },
      "contractPeriod": {
        "status": "FOUND",
        "contractStartDate": "2026-06-01",
        "contractEndDate": "2027-05-31"
      },
      "probation": {
        "status": "FOUND",
        "included": true,
        "months": 1
      },
      "work": {
        "industryCategory": "MANUFACTURING",
        "workplaceRegion": "GYEONGGI_ANSAN",
        "businessCategory": "AUTOMOTIVE_METAL_PARTS",
        "jobCategory": "METAL_PARTS_ASSEMBLY_INSPECTION_PACKAGING"
      },
      "workingHours": {
        "status": "FOUND",
        "startTime": "08:30",
        "endTime": "17:30",
        "overtimeHoursPerDay": 1,
        "maxVariableHoursPerDay": 2,
        "shiftSystem": false
      },
      "breakTime": {
        "status": "FOUND",
        "minutesPerDay": 60
      },
      "holidays": {
        "status": "FOUND",
        "sunday": true,
        "legalHoliday": true,
        "legalHolidayPaid": true,
        "everySaturday": true,
        "otherHoliday": false
      },
      "wage": {
        "status": "FOUND",
        "amount": 2300000,
        "currency": "KRW",
        "period": "MONTHLY",
        "basePay": 2150000,
        "bonusAmount": 0,
        "paymentDay": 10,
        "paymentMethod": "BANK_TRANSFER",
        "overtimeNightHolidayPremiumMentioned": true,
        "fixedAllowances": [
          {
            "type": "PRODUCTION",
            "amount": 100000
          },
          {
            "type": "MEAL",
            "amount": 50000
          }
        ]
      },
      "dormitory": {
        "status": "FOUND",
        "provided": true,
        "typeCategory": "DORMITORY",
        "deductionAmount": 150000
      },
      "meals": {
        "status": "FOUND",
        "provided": true,
        "notProvided": false,
        "providedMeals": ["LUNCH"],
        "deductionAmount": 0
      },
      "signature": {
        "status": "FOUND",
        "signedDate": "2026-06-01",
        "employerSignaturePresent": true,
        "workerSignaturePresent": true
      }
    },
    "evidenceRefs": [
      {
        "evidenceId": "ev-1",
        "fieldName": "wage.amount",
        "page": 1,
        "boundingBox": {
          "x": 0.0,
          "y": 0.0,
          "width": 0.0,
          "height": 0.0
        },
        "confidence": 0.8,
        "maskedExcerpt": "월 통상임금 ([AMOUNT])원"
      }
    ],
    "candidateChecklistItemCodes": [
      "FEA_STANDARD_EMPLOYMENT_CONTRACT",
      "LRA_WRITTEN_CONDITIONS",
      "LRA_MINIMUM_WAGE",
      "LRA_DIRECT_FULL_WAGE_PAYMENT",
      "LRA_REGULAR_PAYDAY",
      "LRA_OVERTIME_NIGHT_HOLIDAY_PREMIUM",
      "LRA_STATUTORY_WORKING_HOURS",
      "LRA_REST_BREAKS",
      "LRA_WEEKLY_PAID_HOLIDAY",
      "FEA_DORMITORY_INFO_DISCLOSURE",
      "FEA_DORMITORY_STANDARD"
    ]
  }
}
```

주의:

- `requestId`는 현재 deterministic hash 성격이다.
- `aiPayloadHash`는 canonical payload hash다.
- `payload`는 `document_extractions.corrected_payload`가 있으면 corrected payload를 우선 사용한다.
- `extractionStatus`는 corrected payload 사용 시 `CORRECTED`일 수 있다.
- `documentHash`는 원문 파일 hash지만 파일 내용은 전달하지 않는다.

## FastAPI -> Spring Response

현재 Spring adapter가 바로 처리할 수 있는 최소 응답:

```json
{
  "requestId": "analysis-request-hash",
  "status": "COMPLETED",
  "summary": "근로계약서상 임금, 근로시간, 휴게시간은 확인되지만 기숙사 공제는 추가 확인이 필요합니다.",
  "riskFlags": [
    {
      "code": "DORMITORY_DEDUCTION_REVIEW_REQUIRED",
      "severity": "MEDIUM",
      "message": "기숙사 공제 금액은 확인되지만 사전 제공 정보와 공제 동의 여부가 함께 확인되어야 합니다.",
      "evidenceIds": ["ev-6"]
    }
  ],
  "analysisResultHash": "sha256-hex"
}
```

Spring 현재 처리 방식:

- `status`가 `COMPLETED`가 아니면 실패 처리
- `summary` 저장
- `riskFlags` 저장
- `analysisResultHash`가 없으면 전체 response JSON hash를 계산해서 저장

향후 확장 응답:

```json
{
  "requestId": "analysis-request-hash",
  "status": "COMPLETED",
  "summary": "...",
  "riskFlags": [],
  "fieldFindings": [],
  "checklistSuggestions": [],
  "recommendedActions": [],
  "citations": [],
  "relatedInstitutions": [],
  "analysisResultHash": "sha256-hex",
  "confidence": 0.86
}
```

이 확장 응답을 저장하려면 Spring DB/API 확장이 필요하다.

## Spring 작업 단위

### P0. 계약 fixture 고정

목표:

- Spring과 FastAPI가 같은 JSON 계약을 보도록 fixture를 고정한다.

작업:

- 현재 request JSON을 `src/test/resources/fixtures/document-analysis-request-employment-contract.json`로 추가한다.
- FastAPI 최소 response fixture를 `src/test/resources/fixtures/document-analysis-response-completed.json`로 추가한다.
- `HttpDocumentAiAnalysisAdapter` 테스트에서 fixture 기반 parsing을 검증한다.
- `docs/offchain-analysis-contract.md`의 논리 schema와 실제 구현 schema 차이를 정리하거나 현재 구현 schema로 맞춘다.

완료 기준:

- fixture 기반 테스트 통과
- AI 팀이 같은 fixture로 FastAPI schema 테스트 가능

### P0. 실제 AI 호출 활성화 준비

목표:

- 로컬/개발 환경에서 placeholder가 아니라 FastAPI를 호출할 수 있게 한다.

작업:

- `.env.example` 또는 배포 문서에 `DOCUMENT_AI_ENABLED`, `DOCUMENT_AI_ENDPOINT` 추가 여부 확인
- 로컬 실행 문서에 아래 조합 추가

```bash
DOCUMENT_AI_ENABLED=true \
DOCUMENT_AI_ENDPOINT=http://localhost:8000/document-analysis \
./gradlew bootRun --args='--spring.profiles.active=local'
```

- AI endpoint가 없거나 5xx일 때 `document_analysis_results.status=FAILED`로 남는지 확인

완료 기준:

- mock FastAPI로 `/api/documents/{documentId}/analysis` 호출 시 실제 HTTP POST 발생
- 실패 시 원본 문서/OCR 상태가 깨지지 않음

### P0. HTTP adapter 운영 보강

목표:

- AI 서비스 장애가 Spring 전체 장애로 번지지 않게 한다.

작업:

- AI 호출 timeout 설정
- 연결 실패/timeout/5xx 응답 처리
- response JSON schema validation
- request/response 전문 로그 금지
- 로그에는 `documentId`, `analysisId`, `requestId`, `aiPayloadHash`, status, error code만 기록
- 필요하면 internal shared token/header 추가

권장 설정:

```env
DOCUMENT_AI_TIMEOUT_MILLIS=30000
DOCUMENT_AI_INTERNAL_TOKEN=
```

완료 기준:

- AI timeout 시 분석 결과만 `FAILED`
- 문서 상태와 OCR 추출 결과는 유지
- 로그에 개인정보/원문 OCR이 남지 않음

### P0. 분석 결과 저장 확장

목표:

- AI가 반환하는 근거/조치/체크리스트 제안을 프론트가 표시할 수 있게 한다.

현재 저장:

- `document_analysis_results.summary`
- `document_analysis_results.risk_flags`
- `document_analysis_results.analysis_result_hash`

추가 후보:

- `document_analysis_findings`
- `document_analysis_checklist_suggestions`
- `document_analysis_recommended_actions`
- `document_analysis_citations`
- 또는 MVP에서는 `document_analysis_results.detail_json` JSON column

MVP 권장:

- 빠르게 가려면 `detail_json` 하나로 전체 AI response를 저장
- 나중에 조회/통계가 필요해지면 별도 테이블로 분리

완료 기준:

- `riskFlags` 외에도 `fieldFindings`, `checklistSuggestions`, `recommendedActions`, `citations`, `relatedInstitutions`를 손실 없이 조회 가능

### P0. 프론트 조회 응답 확장

목표:

- 프론트가 분석 결과 화면을 만들 수 있는 형태로 반환한다.

현재:

- `GET /api/documents/{documentId}/analysis`
- 요약과 risk flags 중심

필요 응답:

```json
{
  "id": "analysis-uuid",
  "documentId": "document-uuid",
  "status": "COMPLETED",
  "summary": "...",
  "riskFlags": [],
  "fieldFindings": [],
  "checklistSuggestions": [],
  "recommendedActions": [],
  "citations": [],
  "relatedInstitutions": [],
  "analyzedAt": "2026-04-30T12:00:00"
}
```

완료 기준:

- 문서 분석 화면에서 위험 항목, 근거, 다음 조치, 기관 연결을 표시할 수 있음

### P1. 분석 트리거 정책 결정

선택지 A: 수동 분석

- 사용자가 OCR 완료 후 `분석 요청` 버튼 클릭
- 비용 제어 쉬움
- 현재 구현과 가까움

선택지 B: OCR 완료 후 자동 분석

- OCR callback 후 extraction이 `EXTRACTED`면 자동으로 AI 분석 요청
- 사용자 경험 좋음
- AI 비용/실패 재시도 관리 필요

권장:

- MVP는 수동 분석 유지
- 데모 안정화 후 자동 분석 옵션을 feature flag로 추가

예시:

```env
DOCUMENT_AI_AUTO_ANALYZE_ON_EXTRACTION=false
```

### P1. 체크리스트 상태 반영 정책

목표:

- AI의 `checklistSuggestions`를 실제 케이스 체크리스트 상태와 어떻게 연결할지 결정한다.

권장 정책:

- AI는 자동으로 checklist를 완료 처리하지 않는다.
- AI는 `suggestedStatus`만 제안한다.
- 사용자가 확인하거나 담당자가 승인해야 checklist 상태를 변경한다.

예시:

```text
AI suggested REVIEW_REQUIRED
-> 프론트 표시
-> 담당자 확인
-> checklist status 변경
```

### P1. 챗봇 streaming 연결

이 작업은 문서 분석 이후에 진행한다.

결정할 것:

- 프론트가 FastAPI `/chat/stream`에 직접 붙을지
- Spring이 `/api/ai/chat/stream` SSE proxy 역할을 할지

권장:

- 인증/권한/케이스 context를 Spring이 들고 있으므로 Spring SSE proxy가 안전하다.

Spring 작업:

- `POST /api/ai/chat/stream`
- JWT 인증
- case/document 접근권한 검증
- FastAPI `/chat/stream`으로 safe context만 전달
- SSE를 프론트로 relay

FastAPI에 보내도 되는 context:

- role
- caseId
- industry
- region
- languageCode
- documentType
- analysis summary/risk code

보내면 안 되는 것:

- 원문 문서
- raw OCR
- 이름/전화번호/외국인등록번호/상세주소

### P2. 임금명세서 확장

지금은 하지 않는다.

나중에 필요한 Spring 작업:

- `PAYSLIP` OCR schema 추가
- payslip extraction payload 정의
- payslip field evidenceRefs 정의
- FastAPI `/document-analysis`가 `documentType=PAYSLIP` 처리 가능해진 뒤 연결

## Spring 작업자에게 그대로 넘길 요약

```text
[Spring 작업 지시서]

목표:
현재 OCR/문서 저장 파이프라인은 유지하고, Spring이 sanitized extraction payload를 FastAPI /document-analysis로 보내 실제 AI 분석 결과를 저장/조회할 수 있게 만든다.

현재 상태:
- EMPLOYMENT_CONTRACT 업로드/OCR/추출은 동작한다.
- /api/documents/{documentId}/analysis는 존재하지만 AI 비활성 시 placeholder를 저장한다.
- FastAPI에는 원문 파일, raw OCR, 개인정보를 보내면 안 된다.

P0 작업:
1. Spring -> FastAPI document-analysis request/response fixture를 테스트 리소스로 고정한다.
2. DOCUMENT_AI_ENABLED=true, DOCUMENT_AI_ENDPOINT 설정으로 실제 HTTP 호출을 검증한다.
3. AI 호출 timeout/error handling/schema validation을 보강한다.
4. AI response의 summary/riskFlags 외 상세 결과를 저장할 구조를 추가한다.
5. GET /api/documents/{documentId}/analysis 응답에 findings/actions/citations/institutions를 포함한다.

P1 작업:
1. OCR 완료 후 자동 분석 여부를 feature flag로 결정한다.
2. AI checklistSuggestions를 실제 checklist 상태에 반영하는 승인 흐름을 설계한다.
3. 챗봇 streaming은 Spring SSE proxy 방식으로 검토한다.

보안 원칙:
- 원문 파일, raw OCR 전문, 이름/전화번호/외국인등록번호/상세주소/회사명은 AI request에 넣지 않는다.
- request/response 전문 로그를 남기지 않는다.
- 로그에는 documentId/requestId/hash/status/error code만 남긴다.

완료 기준:
- mock FastAPI로 실제 POST가 발생한다.
- AI 성공 응답은 DB에 저장되고 프론트 조회 API로 확인된다.
- AI 실패/timeout 시 분석만 FAILED 처리되고 문서/OCR 데이터는 유지된다.
- 기존 OCR/서명/온체인 플로우가 깨지지 않는다.
```

## 병렬 진행 방식

AI 팀이 할 일:

- Neo4j graph schema
- Issue taxonomy seed
- guidebook/statute/precedent ingestion
- FastAPI `/document-analysis` 구현
- Spring request fixture 기반 schema validation

Spring 팀이 할 일:

- 이 문서의 P0 작업
- mock FastAPI로 adapter 검증
- 분석 결과 저장/조회 API 확장
- 프론트가 쓸 response 형태 확정

공유 산출물:

- Spring request fixture
- FastAPI response fixture
- `aiPayloadHash`
- `analysisResultHash`
- `riskFlags.code` 목록
- `checklistItemCode` 목록

## 검증 시나리오

1. Docker MySQL/Redis/OCR worker 실행
2. Spring local profile 실행
3. FastAPI mock 또는 실제 AI service 실행
4. `document-upload-test.html`에서 근로계약서 업로드
5. OCR 완료 확인
6. `POST /api/documents/{documentId}/analysis`
7. Spring logs에서 AI POST 발생 확인
8. `GET /api/documents/{documentId}/analysis`
9. summary/riskFlags/detail fields 확인
10. request/response 전문이 로그에 남지 않았는지 확인

## 관련 문서

- [offchain-analysis-contract.md](offchain-analysis-contract.md)
- [document-onchain-flow.md](document-onchain-flow.md)
- [db-contract.md](db-contract.md)
- AI repo: `../ai/docs/graph-rag-design.md`

