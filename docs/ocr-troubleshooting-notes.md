# OCR Troubleshooting Notes

이 문서는 PaddleOCR 기반 실제 OCR 전환 과정에서 겪은 주요 트러블슈팅을 정리한 작업 노트다. 이후 문서 통합 시 오프체인 분석/OCR 운영 파트에 흡수한다.

## 1. 사용자 온보딩 시점과 OCR 런타임 초기화 분리

### 문제

초기 구조에서는 사용자가 문서를 업로드하고 분석을 요청하는 시점에 OCR 런타임과 모델 준비 비용이 함께 발생했다. 이 경우 첫 사용자가 사실상 모델 초기화 비용을 부담하게 된다.

증상은 다음과 같았다.

- 프론트에서 `처리 중 (n/180)` 상태가 길게 유지됨
- 백엔드 callback 제한시간 안에 결과가 오지 않아 extraction이 `PENDING` 또는 `FAILED`로 남음
- 사용자는 업로드 직후 서비스가 멈춘 것처럼 느끼게 됨

### 판단

OCR 모델 초기화는 사용자 액션의 일부가 아니라 서버 프로세스 준비 단계로 분리해야 한다. 특히 PaddleOCR 계열 모델은 모델 파일 로딩, 런타임 초기화, PDF 페이지 처리 준비가 필요하므로 요청 경로에 얹기 어렵다.

### 조치

- OCR을 Spring 프로세스 내부 구현이 아니라 별도 Docker service인 `ocr-worker`로 분리했다.
- `docker compose up -d` 시 worker가 먼저 뜨고 모델을 로드한다.
- worker는 `/health`에서 준비 상태를 반환한다.
  - 준비 전: warming 상태
  - 준비 후: ready 상태
  - 초기화 실패: failed 상태
- 모델 파일은 Docker volume `paddleocr-models`에 캐시한다.

### 결과

모델 다운로드/초기화 비용은 사용자 문서 업로드 시점에서 분리됐다. 캐시가 존재하는 로컬 환경에서는 worker ready까지 약 1~2초 수준으로 줄었다.

## 2. 동기 callback 대기에서 비동기 Job 처리로 전환

### 문제

실제 OCR 추론은 모델이 준비되어 있어도 CPU 환경에서 2페이지 PDF 기준 약 50~60초가 걸렸다. 초기에는 Spring 요청 흐름에서 callback 완료를 사실상 기다리는 구조였기 때문에, 긴 추론 시간과 HTTP timeout이 충돌했다.

### 판단

OCR 추론은 네트워크 요청-응답 생명주기 안에서 끝나야 하는 작업이 아니다. 백엔드는 OCR 요청 접수 후 즉시 `PENDING` 상태를 반환하고, worker가 완료 후 callback으로 결과를 저장해야 한다.

### 조치

- `POST /ocr/jobs`는 즉시 `202 Accepted`를 반환한다.
- worker 내부 thread에서 OCR을 수행한다.
- 완료 후 Spring callback API로 OCR JSON을 전달한다.
- 프론트는 extraction 상태를 polling한다.
- polling은 초기 `180`회 이후에도 백그라운드 처리 상태를 표시하도록 확장했다.

### 결과

사용자 요청은 즉시 반환되고, OCR 장시간 처리도 백그라운드 작업으로 유지된다. 프론트는 업로드 -> 서명 -> OCR 완료 -> 분석 요청 흐름을 한 화면에서 오케스트레이션할 수 있게 됐다.

## 3. 모델 선택 문제

### 3.1 PPStructureV3

처음에는 문서 구조 분석에 유리한 `PPStructureV3` 계열을 고려했다. 하지만 로컬 CPU 환경에서는 추론 시간이 길고 MVP 사용자 경험에 맞지 않았다.

판단:

- 문서 레이아웃 이해 능력은 좋지만 MVP에서 대기 시간이 과도하다.
- 초기 실제 OCR 연결 검증에는 더 단순한 text detection/recognition pipeline이 적합하다.

### 3.2 PaddleOCR-VL 1.5

이전 임베딩 결과를 만들 때 사용했던 `PaddleOCR-VL 1.5`도 확인했다. 컨테이너에서 실행 시 메모리 사용량이 높아 worker가 종료되는 문제가 있었다.

증상:

- Docker container가 `starting` 상태였다가 사라짐
- inspect 기준 `OOMKilled=true`, exit code `137`

판단:

- 문서 이해 성능은 기대할 수 있으나 현재 로컬 MVP 리소스 기준으로 안정적이지 않다.
- 서버 운영에서 충분한 메모리/GPU 자원이 확보되지 않는 한 기본 OCR worker 모델로 두기 어렵다.

### 3.3 PP-OCRv5 기본 온라인 체험

온라인 체험 결과, 기본 PP-OCRv5는 영어/숫자 영역은 비교적 잘 읽었지만 한글 인식이 부족했다.

문제:

- 고용계약서의 한글 라벨, 체크박스 주변 문구, 숙식/휴일 조건을 안정적으로 읽기 어려움
- 한글이 빠지면 핵심 필드 추출 정확도가 떨어짐

판단:

- 한국어 계약서 MVP에는 Korean recognition model이 필요하다.

### 3.4 최종 MVP 모델

현재 로컬 MVP 기본값:

- text detection: `PP-OCRv5_mobile_det`
- text recognition: `korean_PP-OCRv5_mobile_rec`
- `text_det_limit_side_len=1280`
- `rec_batch_num=8`
- CPU thread: `4`

선택 이유:

- 한글 인식 가능
- server detection 모델보다 mobile detection 모델이 CPU 환경에서 더 현실적
- 2페이지 PDF 기준 약 50~60초로 아직 빠르지는 않지만, timeout 실패 없이 완료 가능

## 4. Paddle 런타임 및 oneDNN 문제

### 문제

모델 실행 중 다음 오류가 발생했다.

```text
NotImplementedError: (Unimplemented) ConvertPirAttribute2RuntimeAttribute not support [pir::ArrayAttribute<pir::DoubleAttribute>]
```

### 판단

Paddle 런타임의 PIR API와 oneDNN 경로에서 특정 attribute 변환이 지원되지 않는 문제로 보였다. OCR 모델 자체의 입력 문제가 아니라 런타임 실행 경로 문제다.

### 조치

worker 환경에서 다음 설정을 적용했다.

```text
FLAGS_enable_pir_api=0
PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT=False
```

### 결과

해당 런타임 오류는 해소됐다. 다만 MKLDNN/oneDNN 가속을 끈 만큼 CPU 성능 최적화 여지는 남아 있다. 운영 환경에서 Paddle/PaddleX 버전 조합과 CPU instruction 지원을 다시 검증해야 한다.

## 5. callback 400 오류와 민감정보 검증 방식

### 문제

OCR worker는 정상적으로 추론을 완료했지만 Spring callback이 `400`을 반환했다.

로그:

```text
callback failed ... status=400 body={"success":false,"error":{"code":"COMMON_400","message":"Sensitive identifiers are not allowed"}}
```

### 원인

민감정보 차단 로직이 JSON 전체 문자열을 대상으로 정규식 검사하고 있었다. 이 때문에 구조화된 payload 내부의 허용 필드까지 전체 문자열 기준으로 오탐할 수 있었다.

### 조치

- JSON을 파싱한 뒤 textual scalar node만 순회해서 검사하도록 변경했다.
- AI로 넘기지 않을 원문 OCR 전문은 저장하지 않고, 추출된 allowlist payload만 DB에 저장한다.

### 결과

callback 저장이 정상화됐고, `document_extractions.extracted_payload`에는 구조화된 계약 조건만 저장된다.

## 6. 추출기 정확도 문제와 일반화 방향

### 문제

OCR 결과는 문서마다 같은 의미의 텍스트를 다르게 반환했다.

예:

- `08시 30분 ~ 17시 30분`
- `from ( 08:30 ) to( 17:30 )`
- OCR 오인식 `08人 30是 ~ 17人 30是`
- 체크박스 주변 문구가 한 줄에 붙거나 여러 블록으로 분리됨

초기 추출기는 특정 문자열에 가까웠기 때문에 정상 문서와 변형/위법 문서를 번갈아 테스트하면 한쪽이 깨질 수 있었다.

### 판단

특정 샘플에 맞춘 adapter식 보정은 위험하다. 필드별로 의미 단위의 일반 규칙을 만들어야 한다.

### 조치

- 근로시간은 한국어/영어/오인식 시간 토큰을 하나의 시간 범위 패턴으로 정규화한다.
- evidence 좌표는 전체 문장이 한 OCR block에 없으면 시작/종료 시간 토큰 block으로 fallback한다.
- 체크박스는 옵션 바로 앞에 체크가 있을 때만 인정한다.
  - `직접 지급`
  - `통장`
  - `컨테이너`
  - `기타 휴일`
- `근처 체크`는 넓게 쓰지 않고, 문맥상 필요한 곳에만 제한적으로 사용한다.
- 정상 문서와 위법/변형 문서를 모두 회귀 fixture로 둔다.

### 결과

정상 문서와 위법/변형 문서 모두 다음 상태로 수렴했다.

- `status=EXTRACTED`
- `reviewRequiredReason=null`
- `workingHours.status=FOUND`
- working hours evidence bbox가 `0,0,0,0`이 아닌 실제 좌표를 가짐
- 체크박스 오판 감소

## 7. 현재 남은 운영 리스크

### 추론 시간

모델 warm-up은 해결됐지만 CPU 추론 자체는 여전히 느리다. 2페이지 PDF 기준 약 50~60초 수준이다.

후속 검토:

- GPU worker 또는 별도 OCR 전용 서버
- PDF 이미지 리사이즈 정책
- 필요한 영역 crop OCR
- worker queue와 concurrency 제한
- 문서 유형별 모델/파서 분기

### 모델/버전 고정

PaddleOCR/PaddleX/Paddle 런타임 조합에 따라 동작이 달라질 수 있다.

후속 검토:

- Docker image digest 또는 dependency lock
- 모델 캐시 볼륨 초기화 절차
- worker startup health와 readiness timeout 정책

### OCR 원문 보존 정책

현재 원칙은 필터링 전 OCR 결과를 DB에 저장하지 않는 것이다. 디버깅 편의와 개인정보 보호 사이에 trade-off가 있다.

후속 검토:

- 로컬 개발 전용 redacted debug dump
- 운영에서는 hash/error code/field status 중심 로그
- evidenceRefs 품질 확인용 제한적 메타데이터만 보존

## 8. 문서 통합 시 반영 위치

후속 문서 정리 시 다음 위치로 분산 반영한다.

- `docs/document-onchain-flow.md`
  - 업로드 후 OCR 비동기 job과 프론트 오케스트레이션 설명
- `docs/offchain-analysis-contract.md`
  - OCR 결과 정규화, sanitizer, AI payload 경계
- `docs/current-architecture.md`
  - 실제 OCR worker 도입으로 placeholder가 일부 해소된 점과 남은 AI 레이어 부채
- `README.md`
  - 로컬 실행 및 worker readiness, 모델 기본값, 주요 환경 변수
