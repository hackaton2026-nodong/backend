# Offchain Analysis Contract

이 문서는 문서 업로드 후 OCR/parser와 AI 분석 레이어 사이에서 오가는 데이터 계약을 정의한다. 핵심 원칙은 원문 문서와 필터링 전 OCR 결과를 AI 레이어로 전달하지 않는 것이다.

## Scope

현재 구현의 `POST /api/documents/{documentId}/analysis`는 placeholder다. 이 문서는 향후 placeholder를 실제 OCR/parser 및 AI 분석 RPC로 대체할 때 지켜야 할 입력/출력 규격이다.

업로드 후 공통 기반은 아래와 같다.

```text
document upload
-> local/original document storage
-> sha256 hash
```

그 이후 오프체인 분석은 온체인 증명과 독립적으로 진행한다.

```text
HASHED document
-> OCR/parser
-> sanitize/mask
-> build AI request
-> AI analysis
-> persist analysis result
```

## Non-Negotiable Rules

- 원문 파일은 지정된 문서 저장소에만 존재한다.
- AI request에는 `storageKey`, 파일 바이너리, 원문 텍스트, 필터링 전 OCR 전문을 넣지 않는다.
- 필터링 전 OCR/parser 결과는 메모리 처리에 한정하고 DB, 로그, 파일에 남기지 않는다.
- AI 레이어는 내부망 여부와 무관하게 비신뢰 소비자로 간주한다.
- AI payload는 allowlist 기반으로 생성한다. 명시적으로 허용하지 않은 필드는 제거한다.
- 로그에는 request/response 전문을 남기지 않고 `documentId`, `analysisId`, `status`, hash, error code 중심으로 기록한다.

## Data Flow

1. `Document`가 `HASHED` 상태인지 확인한다.
2. 원본 파일은 storage adapter를 통해 OCR/parser에만 전달한다.
3. OCR/parser는 필터링 전 텍스트를 메모리에서만 생성한다.
4. sanitizer가 개인정보, 사업장 식별정보, 상세주소, 문서번호, 계좌번호 등 식별 가능 값을 제거하거나 마스킹한다.
5. AI request builder가 허용된 근로조건 필드와 근거 참조만 조합한다.
6. AI analysis port가 request를 전송한다.
7. AI response를 검증한 뒤 `document_analysis_results`와 후속 분석 테이블에 저장한다.

## Field Policy

### Drop

AI payload에서 제거한다.

- 원본 파일 경로 또는 `storageKey`
- 원문 OCR 전문
- 이름, 서명, 도장, 사진
- 전화번호, 이메일, 메신저 ID
- 주민등록번호, 외국인등록번호, 여권번호, 비자 문서번호
- 계좌번호, 은행 계좌 식별자
- 사업자등록번호, 법인등록번호
- 회사명, 대표자명, 담당자명
- 상세주소, 사업장 전체 주소, 기숙사 상세 주소

### Keep

법령/체크리스트 판단에 필요한 근로조건 값으로 보존한다.

- `documentId`
- `caseId`
- `documentHash`
- `documentType`
- 업종 category
- 지역 category
- 계약 시작일, 계약 종료일
- 임금 금액, 임금 단위, 지급 주기
- 소정근로시간, 연장/야간/휴일 조건
- 휴게시간
- 휴일/휴가 조건
- 업무 또는 직무 category
- 기숙사 제공 여부
- 숙식비 공제 여부와 공제 금액
- 보험/교육/체류자격 관련 체크 여부

### Generalize

식별 가능성을 낮추기 위해 범주화한다.

- 근무장소: 상세주소 대신 region 또는 workplace category
- 국적/언어: 개인 식별값이 아니라 `languageCode`나 broad country/region category
- 체류자격: 실제 번호가 아니라 visa status category
- 사업장 정보: 회사명 대신 industry, region, workplace size/category

### Masked Evidence

AI 설명 품질과 추적성을 위해 제한적으로 전달한다.

- `evidenceId`
- `fieldName`
- `page`
- `boundingBox`
- `confidence`
- `maskedExcerpt`

`maskedExcerpt`는 식별자가 제거된 짧은 문구여야 한다. 원문 문단 전체를 전달하지 않는다.

## AI Request Schema

```json
{
  "requestId": "analysis-request-uuid",
  "document": {
    "documentId": "document-uuid",
    "caseId": "case-uuid",
    "documentHash": "sha256-hex",
    "documentType": "EMPLOYMENT_CONTRACT",
    "issuedAt": "2026-01-01",
    "expiresAt": "2027-01-01"
  },
  "caseContext": {
    "industry": "Manufacturing",
    "region": "Seoul",
    "languageCode": "ko",
    "workerStatusCategory": "FOREIGN_WORKER"
  },
  "contractTerms": {
    "contractStartDate": "2026-01-01",
    "contractEndDate": "2027-01-01",
    "wage": {
      "amount": 2500000,
      "currency": "KRW",
      "period": "MONTHLY",
      "paymentDay": "MONTH_END"
    },
    "workingHours": {
      "hoursPerDay": 8,
      "hoursPerWeek": 40,
      "overtimeMentioned": true,
      "nightWorkMentioned": false,
      "holidayWorkMentioned": false
    },
    "breakTime": {
      "minutesPerDay": 60
    },
    "work": {
      "jobCategory": "MANUFACTURING",
      "workplaceRegion": "Seoul"
    },
    "dormitory": {
      "provided": true,
      "deductionAmount": 200000
    }
  },
  "evidenceRefs": [
    {
      "evidenceId": "ev-1",
      "fieldName": "wage.amount",
      "page": 1,
      "boundingBox": {
        "x": 0.12,
        "y": 0.34,
        "width": 0.32,
        "height": 0.04
      },
      "confidence": 0.94,
      "maskedExcerpt": "월급 [AMOUNT]원"
    }
  ],
  "checklistContext": {
    "catalogCode": "MOEL_FOREIGN_WORKER_EMPLOYMENT_MANAGEMENT",
    "candidateItemCodes": [
      "FEA_STANDARD_EMPLOYMENT_CONTRACT",
      "LRA_WRITTEN_CONDITIONS",
      "MWA_MINIMUM_WAGE"
    ]
  },
  "outputRequest": {
    "languageCode": "ko",
    "includeChecklistSuggestions": true,
    "includeEvidenceRefs": true
  }
}
```

## AI Response Schema

```json
{
  "requestId": "analysis-request-uuid",
  "status": "COMPLETED",
  "summary": "근로계약서의 주요 근로조건은 대체로 확인되지만, 기숙사 공제와 연장근로 조건은 추가 확인이 필요합니다.",
  "riskFlags": [
    {
      "code": "DORMITORY_DEDUCTION_REVIEW_REQUIRED",
      "severity": "MEDIUM",
      "message": "기숙사 공제 금액은 확인되지만 사전 제공 정보와 공제 동의 여부가 함께 확인되어야 합니다.",
      "evidenceIds": ["ev-1"]
    }
  ],
  "fieldFindings": [
    {
      "fieldName": "wage.amount",
      "status": "FOUND",
      "normalizedValue": "2500000 KRW MONTHLY",
      "confidence": 0.94,
      "evidenceIds": ["ev-1"]
    }
  ],
  "checklistSuggestions": [
    {
      "checklistItemCode": "LRA_WRITTEN_CONDITIONS",
      "suggestedStatus": "REVIEW_REQUIRED",
      "reason": "서면 명시 대상 근로조건은 확인되지만 일부 조건의 증빙 검토가 필요합니다.",
      "evidenceIds": ["ev-1"]
    }
  ],
  "recommendedActions": [
    {
      "targetRole": "EMPLOYER",
      "actionType": "REVIEW_DOCUMENT",
      "message": "기숙사 공제와 관련된 별도 안내 또는 동의 자료를 확인하세요."
    }
  ],
  "confidence": 0.87
}
```

## Persistence Policy

MVP의 기존 `document_analysis_results`는 요약 결과 저장소로 유지한다.

- `extracted_text_hash`: 마스킹/정규화된 AI request 본문 또는 normalized terms의 hash
- `analysis_result_hash`: AI response canonical JSON hash
- `summary`: 사용자에게 표시 가능한 요약
- `risk_flags`: AI response의 `riskFlags` JSON

후속 구현에서 별도 테이블을 추가한다면 저장 대상은 필터링 후 산출물만 허용한다.

- `document_sanitized_inputs`: 마스킹된 AI request와 hash
- `document_analysis_findings`: field findings, risk flags, checklist suggestions
- `document_evidence_refs`: page, boundingBox, confidence, maskedExcerpt

필터링 전 raw OCR text 저장 테이블은 만들지 않는다.

## Validation Scenarios

- 이름, 전화번호, 이메일, 외국인등록번호, 여권번호, 사업자번호, 상세주소가 AI request에 포함되지 않아야 한다.
- 임금, 근로시간, 휴게시간, 계약기간, 기숙사 제공 여부는 AI request에 보존되어야 한다.
- `maskedExcerpt`만으로 원문 식별자를 복원할 수 없어야 한다.
- 로그에 AI request/response 전문이 남지 않아야 한다.
- AI response가 알 수 없는 `evidenceId`를 참조하면 저장 전에 거부하거나 `FAILED` 처리해야 한다.
- AI response의 `checklistItemCode`는 reference checklist catalog에 존재해야 한다.

## Future Work

- OCR/parser 포트와 sanitizer 포트를 분리한다.
- sanitizer allowlist와 masking pattern을 문서 유형별로 관리한다.
- 계약서 외 문서 유형(`PAYSLIP`, `VISA`, `RESIDENCE_PROOF`)은 별도 request schema를 추가한다.
- 사용자 화면에는 분석 결과와 원문 근거 위치를 연결하되, 원문 노출 권한은 기존 문서 접근 권한을 따른다.
