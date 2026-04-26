# Backend

조직 단위 사용자 관리, 케이스 관리, 문서 업로드를 담당하는 Spring Boot 백엔드입니다.

## 기술 스택

- Java 21
- Spring Boot 3.4.4
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Data Redis
- MySQL
- H2
- JWT (`jjwt`)
- Springdoc OpenAPI
- Docker Compose

## 프로젝트 구조

```text
src/main/java/com/kworkerharmony/backend
├── auth
├── cases
├── country
├── document
│   ├── dto
│   ├── infrastructure
│   └── port
├── enterprise
├── global
└── user
```

- `auth`: 회원가입, 로그인, 토큰 재발급, 로그아웃
- `enterprise`: 회사, 초대코드, 회사 사용자 관리
- `cases`: 케이스 생성, 멤버 연결, 조회
- `document`: 문서 업로드, 조회, 로컬 저장, 해시 처리
- `global`: 보안, 설정, 예외 처리, 공통 응답

## 핵심 도메인

### Enterprise

- 회사 단위 식별 주체
- 관리자 가입 시 함께 생성
- 초대코드 가입 시 기존 회사에 연결

### User

- 회사 소속 사용자
- `ADMIN`, `EMPLOYER`, `WORKER` 역할 사용

### Case

- 회사 단위로 생성되는 작업 단위
- 같은 회사 사용자만 접근 가능

### Document

- 케이스에 소속되는 문서
- 업로드 후 로컬 파일 저장
- 저장 후 SHA-256 해시 생성
- 해시를 기준으로 오프체인 분석과 온체인 증명 플로우가 분기됨

## 현재 구현 범위

- 관리자 회원가입 및 회사 생성
- 초대코드 기반 회사 가입
- JWT access token / refresh token 발급
- Redis 기반 refresh token 저장
- 케이스 생성 및 조회
- 케이스 문서 업로드
- 문서 목록 / 상세 조회
- 로컬 파일 저장
- SHA-256 해시 생성
- 문서 해시 기반 EIP-712 서명 요청/저장 MVP
- Stub relayer 기반 문서 앵커링 MVP
- 오프체인 분석 결과 placeholder 저장

문서 업로드와 앵커링 MVP에서 현재 확인 가능한 주요 상태 전이는 아래와 같습니다.

- `UPLOADED`
- `STORED`
- `HASHED`
- `SIGNATURE_REQUESTED`
- `SIGNED`
- `ANCHOR_PENDING`
- `ANCHORED_ON_CHAIN`
- `ANCHOR_FAILED`

현재 범위의 중요한 경계는 아래와 같습니다.

- 실제 파일 저장과 SHA-256 해시 생성은 동작합니다.
- 지갑 서명 요청/제출 API와 Stub 앵커링 API는 동작합니다.
- 실제 Sepolia 트랜잭션 전송은 아직 구현하지 않았습니다.
- 오프체인 분석 API는 placeholder이며 실제 OCR/AI 호출은 아직 구현하지 않았습니다.
- 오프체인 분석에서 원문 파일과 필터링 전 OCR 결과는 AI 레이어로 전달하지 않으며, 세부 입력 규격은 `docs/offchain-analysis-contract.md`를 따릅니다.
- 정적 테스트 페이지는 MVP 검증용이며, 실제 제품 UX에서는 업로드 후 분석 자동 시작과 서명 후 앵커링 자동 진행으로 분리하는 것이 좋습니다.

## 로컬 실행

인프라 실행:

```bash
docker compose up -d
```

백엔드 실행:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

`local` 프로필의 기본값은 Docker Compose 기준으로 맞춰져 있어 별도 환경변수 없이 실행 가능합니다.
로컬 MySQL 또는 다른 접속 정보를 사용할 경우 아래 환경 변수를 본인 환경에 맞게 지정하면 됩니다.

```bash
LOCAL_MYSQL_URL='jdbc:mysql://localhost:3306/backend?serverTimezone=Asia/Seoul&characterEncoding=UTF-8' \
LOCAL_MYSQL_USERNAME=root \
LOCAL_MYSQL_PASSWORD=1234 \
./gradlew bootRun --args='--spring.profiles.active=local'
```

`dev` 프로필로 실행할 경우:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

문서 업로드 테스트 페이지:

- `http://localhost:8080/document-upload-test.html`
- `http://localhost:8080/dashboard-api-preview.html`

로컬 시드 계정:

- `admin.local@kworkerharmony.com` / `password123`
- `employer.local@kworkerharmony.com` / `password123`
- `worker.local@kworkerharmony.com` / `password123`

## 주요 환경 변수

- `LOCAL_MYSQL_URL`, `LOCAL_MYSQL_USERNAME`, `LOCAL_MYSQL_PASSWORD`
- `DEV_MYSQL_URL`, `DEV_MYSQL_USERNAME`, `DEV_MYSQL_PASSWORD`
- `MYSQL_URL`, `MYSQL_USERNAME`, `MYSQL_PASSWORD` (`dev` 프로필 fallback)
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `DOCUMENT_STORAGE_ROOT`
- `DOCUMENT_CHAIN_ID`
- `DOCUMENT_CONTRACT_ADDRESS`
- `DOCUMENT_DOMAIN_NAME`
- `DOCUMENT_DOMAIN_VERSION`
- `DOCUMENT_SIGNATURE_TTL_SECONDS`

기본 문서 저장 경로는 프로젝트 루트 기준 `./storage/documents`입니다.

## 테스트

```bash
./gradlew test
```

테스트는 H2 기반으로 동작하며, 로컬 실행은 MySQL과 Redis를 사용합니다.

## 수동 확인 포인트

- 회원가입 후 사용자와 회사가 생성되는지
- 로그인 후 access token 발급이 되는지
- 케이스 생성이 되는지
- 문서 업로드 후 `documents` 테이블에 데이터가 쌓이는지
- 업로드 파일이 `storage/documents` 아래 저장되는지
- 업로드 응답에서 `stored=true`, `status=HASHED`가 반환되는지
- `document-upload-test.html`에서 `Create Signature Request`, `Connect Wallet`, `Sign EIP-712`, `Submit Signature`, `Anchor Document`, `Get Anchor` 순서로 Stub 앵커링 결과가 반환되는지
- `Create Analysis`로 placeholder 분석 결과가 저장되는지

## 예시 시나리오

1. `docker compose up -d`
2. `./gradlew bootRun`
3. Swagger에서 `POST /api/auth/signup` 호출

```json
{
  "email": "admin@test.com",
  "password": "password123",
  "name": "Admin User",
  "userType": "EMPLOYER",
  "countryCode": "KR",
  "companyName": "Harmony Co",
  "companyBusinessNumber": "123-45-67890",
  "companyIndustry": "Manufacturing",
  "companyCountry": "KR"
}
```

4. Swagger에서 `POST /api/auth/login` 호출

```json
{
  "email": "admin@test.com",
  "password": "password123"
}
```

5. Swagger에서 `POST /api/cases` 호출

```json
{
  "industry": "Manufacturing",
  "region": "Seoul"
}
```

6. `http://localhost:8080/document-upload-test.html` 접속 후 로그인 응답의 `accessToken`, 케이스 생성 응답의 `data.id`를 넣고 파일 1건 업로드
7. `storage/documents` 아래 실제 파일 생성 여부 확인
8. MetaMask를 Sepolia로 맞춘 뒤 `Create Signature Request` -> `Connect Wallet` -> `Sign EIP-712` -> `Submit Signature` -> `Anchor Document` -> `Get Anchor` 순서로 Stub 앵커링을 확인
9. `Create Analysis`로 placeholder 분석 저장 확인

## 참고 문서

- [current-architecture.md](docs/current-architecture.md)
- [db-contract.md](docs/db-contract.md)
- [document-onchain-flow.md](docs/document-onchain-flow.md)
- [offchain-analysis-contract.md](docs/offchain-analysis-contract.md)
