# Document Onchain/Offchain Flow

이 문서는 문서 업로드 이후 해시 기반 지갑 서명, 온체인 앵커링 준비, 오프체인 분석 결과 저장까지의 현재 구현 범위를 정리한다.

## Current Scope

현재 구현은 MVP 검증용 수직 슬라이스다.

- 원본 파일은 로컬 저장소에 저장하고, `documents.sha256_hash`를 문서 무결성 기준값으로 사용한다.
- 프론트는 `document-upload-test.html`에서 MetaMask 호환 지갑 연결과 EIP-712 서명 요청을 수행한다.
- 백엔드는 서명 요청 payload를 생성하고, 제출된 서명을 저장하며, 서버 기준 payload로 `typedDataHash`를 재계산한다.
- 앵커링은 `DocumentAnchorRelayerPort` 뒤의 Stub adapter가 처리한다. 실제 Sepolia 트랜잭션은 아직 전송하지 않는다.
- 오프체인 OCR 추출은 Docker Compose의 PaddleOCR worker와 callback API로 실제 동작한다.
- OCR callback 결과는 필터링/정규화되어 `document_extractions`에 저장된다. 원본 OCR JSON은 저장하지 않고 `source_result_hash`만 남긴다.
- 오프체인 분석은 `document_analysis_results` 모델과 API로 placeholder 결과를 저장한다. 설정을 켜면 저장된 sanitized extraction payload를 AI 레이어 endpoint로 `POST`하는 구조로 확장된다. 입력/출력 계약은 [offchain-analysis-contract.md](offchain-analysis-contract.md)를 따른다.

문서 업로드 후 플로우는 의도적으로 두 갈래로 분리한다.

- 오프체인 추출 플로우: `HASHED` 문서를 PaddleOCR worker로 처리한 뒤 필터링/마스킹된 allowlist payload를 `document_extractions`에 저장한다.
- 오프체인 분석 플로우: 저장된 extraction payload를 기준으로 AI 분석 레이어가 분석하고 `document_analysis_results`에 저장한다. 현재 AI 분석은 placeholder다.
- 온체인 증명 플로우: 같은 `documentHash`에 대해 사용자의 EIP-712 서명을 받고, 서버 relayer가 앵커링 결과를 `document_anchors`에 저장한다. 현재는 Stub relayer다.

테스트 페이지는 MVP 검증 편의를 위해 업로드, OCR polling, 지갑 서명, 앵커링, 분석 placeholder 저장을 한 화면에 모아 둔 오케스트레이터다. 실제 제품 UX에서는 개발자용 입력/상태 출력을 줄이고 동일한 연쇄 흐름을 사용자 친화적으로 다듬는 작업이 남아 있다.

## Status Flow

- `DocumentStatus`: `UPLOADED`, `STORED`, `HASHED`, `SIGNATURE_REQUESTED`, `SIGNED`, `ANCHOR_PENDING`, `ANCHORED_ON_CHAIN`, `ANCHOR_FAILED`, `FAILED`
- `DocumentSignatureStatus`: `REQUESTED`, `SIGNED`, `EXPIRED`, `REJECTED`
- `DocumentAnchorStatus`: `PENDING`, `ANCHORED`, `FAILED`
- `DocumentAnalysisStatus`: `PENDING`, `COMPLETED`, `FAILED`

## API Contract

모든 API는 인증이 필요하며 공통 응답 래퍼는 `ApiResponse<T>`다.

### `GET /api/documents/{documentId}/signature-request`

문서가 `HASHED`, `SIGNATURE_REQUESTED`, `SIGNED` 상태일 때 EIP-712 서명 요청 payload를 생성한다.

```json
{
  "success": true,
  "data": {
    "documentId": "doc-uuid",
    "expectedChainId": 11155111,
    "domain": {
      "name": "KWorkerHarmonyDocument",
      "version": "1",
      "chainId": 11155111,
      "verifyingContract": "0x1111111111111111111111111111111111111111"
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
      "documentId": "doc-uuid",
      "caseId": "case-uuid",
      "documentHash": "0x...",
      "documentType": "EMPLOYMENT_CONTRACT",
      "signerUserId": 3,
      "nonce": "0x...",
      "deadline": 1770000000
    },
    "typedDataHash": "0x..."
  }
}
```

### `POST /api/documents/{documentId}/signatures`

프론트가 지갑 서명 결과를 백엔드에 제출한다.

```json
{
  "walletAddress": "0x1111111111111111111111111111111111111111",
  "chainId": 11155111,
  "signature": "0xabcdef",
  "typedDataHash": "0xOptionalClientCalculatedHash",
  "nonce": "0x..."
}
```

```json
{
  "success": true,
  "data": {
    "signatureId": "sig-uuid",
    "documentId": "doc-uuid",
    "walletAddress": "0x1111111111111111111111111111111111111111",
    "status": "SIGNED",
    "signedAt": "2026-04-25T22:00:00"
  }
}
```

백엔드는 프론트가 제출한 `typedDataHash`를 신뢰하지 않는다. 저장된 signature request payload 기준으로 `typedDataHash`를 다시 계산하고, nonce/deadline/chainId를 검증한다. 현재 MVP는 실제 ECDSA signer recovery 검증 전 단계이며, 서명 형식과 서버 payload 일관성만 검증한다.

### `POST /api/documents/{documentId}/anchor`

저장된 서명을 기준으로 서버 relayer 앵커링을 요청한다.

```json
{
  "signatureId": "sig-uuid"
}
```

```json
{
  "success": true,
  "data": {
    "anchorId": "0x...",
    "documentId": "doc-uuid",
    "status": "ANCHORED_ON_CHAIN",
    "contractAddress": "0x1111111111111111111111111111111111111111",
    "chainId": 11155111,
    "txHash": "0x...",
    "blockNumber": 1,
    "anchoredAt": "2026-04-25T22:03:00"
  }
}
```

`POST /anchor`는 idempotent하게 동작한다.

- 이미 `PENDING`이면 새 트랜잭션을 만들지 않고 기존 `anchorId`, `txHash`, `status`를 반환한다.
- 이미 `ANCHORED`이면 기존 anchor 결과를 반환한다.
- `FAILED`는 동일 API 재호출로 재시도할 수 있다.

### Analysis APIs

- `POST /api/documents/{documentId}/extractions/paddle-ocr`: 인증 사용자가 직접 PaddleOCR JSON을 제출해 extraction을 생성한다. 테스트/수동 검증용이다.
- `GET /api/documents/{documentId}/extractions`: 저장된 extraction 상태와 payload를 조회한다.
- `PATCH /api/documents/{documentId}/extractions`: 사용자 보정 payload를 저장한다.
- `POST /api/internal/documents/{documentId}/ocr-result`: OCR worker callback endpoint다. callback token으로 보호되며 원본 OCR JSON을 받아 구조화 payload만 저장한다.
- `POST /api/documents/{documentId}/analysis`: 오프체인 분석 결과 생성을 시작한다. 현재는 실제 AI 호출 없이 저장된 extraction payload 기준 placeholder 결과를 저장한다.
- `GET /api/documents/{documentId}/analysis`: 저장된 분석 결과를 조회한다.

실제 AI 분석 구현 시 AI 레이어에는 원문 파일, `storageKey`, 필터링 전 OCR 전문을 전달하지 않는다. 백엔드는 저장된 sanitized extraction payload와 메타데이터만 envelope로 구성해 AI 레이어에 `POST`한다.

## EIP-712 Wallet Flow

프론트의 책임은 지갑 연결과 EIP-712 서명 요청까지다.

1. `window.ethereum`으로 MetaMask 호환 지갑을 감지한다.
2. `eth_requestAccounts`로 wallet address를 가져온다.
3. 백엔드의 `GET /signature-request` 응답을 그대로 사용해 typed data를 구성한다.
4. `eth_signTypedData_v4`로 사용자의 지갑 서명을 요청한다.
5. 서명값, wallet address, chainId, nonce를 `POST /signatures`로 제출한다.

현재 기본 chainId는 Sepolia `11155111`이다. MetaMask가 잘못된 네트워크에 연결되어 있으면 static test page에서 서명을 막는다.

## Smart Contract Target Interface

실제 컨트랙트 어댑터는 아직 구현하지 않았지만, 백엔드 relayer 포트는 아래 인터페이스를 전제로 분리되어 있다.

```solidity
function anchorDocument(
    bytes32 documentHash,
    bytes32 caseIdHash,
    address signer,
    bytes32 typedDataHash,
    bytes calldata signature,
    bytes32 nonce,
    uint256 deadline
) external returns (bytes32 anchorId);

event DocumentAnchored(
    bytes32 indexed anchorId,
    bytes32 indexed documentHash,
    bytes32 indexed caseIdHash,
    address signer,
    bytes32 typedDataHash,
    bytes32 nonce,
    uint256 anchoredAt
);
```

백엔드는 EIP-712 message의 `caseId` 문자열을 컨트랙트 호출 전에 `caseIdHash = hash(caseId)`로 변환한다. 현재 MVP 구현은 Java 표준 `SHA-256` 기반 helper를 사용한다. 실제 EVM 컨트랙트 연동 시에는 Solidity와 동일한 `keccak256` 계산으로 교체해야 한다.

## DB Model

### `document_signatures`

서명 요청과 제출된 지갑 서명을 저장한다.

- 주요 필드: `document_id`, `user_id`, `wallet_address`, `chain_id`, `verifying_contract`, `typed_data_hash`, `client_typed_data_hash`, `signature`, `signature_hash`, `nonce`, `deadline`, `status`, `signed_at`
- Unique: `(document_id, user_id, wallet_address)`, `(chain_id, verifying_contract, nonce)`, `(typed_data_hash)`, `(signature_hash)`

### `document_anchors`

서버 relayer 앵커링 요청과 결과를 저장한다.

- 주요 필드: `document_id`, `signature_id`, `chain_id`, `contract_address`, `anchor_id`, `document_hash`, `case_id_hash`, `tx_hash`, `block_number`, `status`, `retry_count`, `last_error_message`, `anchored_at`
- Unique: `(chain_id, contract_address, anchor_id)`, `tx_hash` only when `tx_hash IS NOT NULL`

### `document_analysis_results`

오프체인 OCR/AI 분석 결과 저장을 위한 테이블이다.

- 주요 필드: `document_id`, `status`, `extracted_text_hash`, `analysis_result_hash`, `summary`, `risk_flags`, `analyzed_at`
- Unique: `(document_id)`
- `extracted_text_hash`는 필터링 전 OCR 전문의 hash가 아니라, 마스킹/정규화된 AI request 또는 normalized terms의 hash로 사용한다.

### `document_extractions`

PaddleOCR callback 결과에서 근로계약서 분석에 필요한 필드만 추출해 저장한다.

- 주요 필드: `document_id`, `status`, `schema_version`, `source_engine`, `source_result_hash`, `extracted_payload`, `corrected_payload`, `ai_payload_hash`, `review_required_reason`, `extracted_at`, `corrected_at`
- Unique: `(document_id)`
- `source_result_hash`는 저장하지 않는 원본 OCR JSON의 canonical hash다.
- `extracted_payload`와 `corrected_payload`는 개인정보와 원문 OCR 전문을 제외한 구조화 계약 조건만 포함한다.

## Local Manual Test

1. 로컬 인프라를 실행한다.

```bash
docker compose up -d
```

2. IDE에서 `BackendApplication`을 실행한다. MetaMask 서명에서 zero address 문제가 생기지 않도록 실행 설정의 환경 변수에 `DOCUMENT_CONTRACT_ADDRESS=0x1111111111111111111111111111111111111111`를 추가한다.

3. 브라우저에서 테스트 페이지에 접속한다.

```text
http://localhost:8080/document-upload-test.html
```

4. access token, case ID, 파일을 입력해 문서를 업로드한다.
5. 화면은 OCR worker callback을 polling하고, 동시에 지갑 서명 플로우를 진행한다.
6. MetaMask 네트워크를 Sepolia로 맞춘다.
7. 지갑 서명이 완료되면 서명 제출과 Stub 앵커링을 이어서 확인한다.
8. extraction이 `EXTRACTED` 또는 `CORRECTED` 상태가 되면 분석 placeholder 저장을 확인한다.

## Known Limits

- 실제 Sepolia RPC 호출과 `web3j` 기반 relayer 어댑터는 아직 구현하지 않았다.
- 실제 ECDSA signer recovery 검증은 아직 구현하지 않았다.
- 현재 `typedDataHash`, `caseIdHash`, `anchorId` 계산은 MVP 내부 검증용 SHA-256 helper를 사용한다. 실제 Solidity 검증과 맞추려면 keccak/EIP-712 canonical hashing 구현으로 교체해야 한다.
- 오프체인 OCR 추출은 PaddleOCR worker 기반으로 동작한다. 다만 CPU 추론 시간은 2페이지 PDF 기준 약 50~60초 수준이라 운영 환경에서는 worker 리소스와 queue 정책이 필요하다.
- 오프체인 AI 분석은 placeholder다. 후속 구현은 원문 비전달, 필터링 전 OCR 비저장, 마스킹 근거 참조 정책을 유지해야 한다.
- DB 변경은 `docker/mysql/init/01-schema.sql`에 반영되어 있으나, 운영 마이그레이션 도구는 아직 없다.

## Next Development Points

- `DocumentAnchorRelayerPort`의 Stub 구현을 web3j 기반 Sepolia relayer로 교체한다.
- EIP-712 canonical hash와 Solidity `keccak256` 계산을 맞추고, 서버에서 ECDSA signer recovery를 수행한다.
- `caseIdHash`, `anchorId`, `typedDataHash`를 실제 컨트랙트 계산식과 동일하게 정렬한다.
- relayer private key, RPC URL, gas/nonce 정책을 환경변수와 운영 설정으로 분리한다.
- AI 분석 레이어 POST 연동을 활성화해 placeholder 분석을 실제 분석 결과로 대체한다.
- 프론트 오케스트레이션을 제품 UX로 정리해 업로드, OCR, 서명, 앵커링, 분석 요청이 자연스럽게 이어지게 한다.
