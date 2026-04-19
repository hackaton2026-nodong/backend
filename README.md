# Backend

조직 단위 사용자 관리와 케이스 기반 문서 처리를 담당하는 Spring Boot 백엔드입니다.

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

## 프로젝트 구조

```text
src/main/java/com/kworkerharmony/backend
├── auth
├── cases
├── country
├── document
│   ├── infrastructure
│   └── port
├── enterprise
├── global
└── user
```

- `auth`: 회원가입, 로그인, 토큰 재발급, 로그아웃
- `enterprise`: 회사 정보, 초대코드, 회사 사용자 관리
- `cases`: 케이스 생성, 멤버 연결, 케이스 조회
- `document`: 문서 업로드, 문서 조회, 저장/해시/앵커 포트
- `global`: 보안, 공통 응답, 예외 처리, 공통 설정

## 핵심 도메인

### Company

현재 코드에서는 `Enterprise` 엔티티가 회사 역할을 담당합니다.

- `name`
- `businessNumber`
- `industry`
- `country`
- `status`

회사 초대코드는 `CompanyInviteCode`로 관리합니다.

- `code`
- `expiresAt`
- `maxUses`
- `usedCount`
- `active`
- `defaultRole`

### User

- `email`
- `passwordHash`
- `name`
- `role`
- `userType`
- `status`
- `country`
- `enterprise`

관리자 가입 시 회사가 함께 생성되고, 초대코드 가입 시 기존 회사에 연결됩니다.

### Case

- `enterprise`
- `employer`
- `worker`
- `status`
- `industry`
- `region`

케이스는 회사 단위로 생성되고, 같은 회사 사용자만 접근할 수 있습니다.

### Document

- `caseId`
- `uploaderUserId`
- `documentType`
- `status`
- `originalFileName`
- `storageKey`
- `mimeType`
- `fileSize`
- `sha256Hash`
- `anchoredTxId`
- `issuedAt`
- `expiresAt`

문서는 케이스에 소속되며 로컬 파일 저장 후 SHA-256 해시를 생성합니다.

## 구현된 기능

### 인증 및 가입

- 관리자 가입 시 회사 생성과 사용자 연결
- 초대코드 기반 회사 가입
- JWT access/refresh token 발급
- Redis 기반 refresh token 저장
- 로그아웃 시 refresh token 제거 및 access token 블랙리스트 처리

### 회사 관리

- 회사 생성
- 초대코드 발급
- 초대코드 기반 회사 참여
- 회사 소속 사용자 조회

### 케이스 관리

- 케이스 생성
- 활성 케이스 조회
- 케이스 상세 조회
- 케이스 멤버 연결

### 문서 처리

- 케이스별 문서 업로드
- 케이스별 문서 목록 조회
- 문서 상세 조회
- 로컬 디스크 저장
- SHA-256 해시 생성
- 문서 상태 전이
  - `UPLOADED`
  - `STORED`
  - `HASHED`

## 권한 규칙

- 모든 비공개 API는 JWT 인증이 필요합니다.
- 같은 회사 사용자만 같은 케이스에 접근할 수 있습니다.
- 문서 업로드와 문서 조회는 케이스 당사자 또는 회사 관리자만 가능합니다.
- 회사 사용자 조회와 초대코드 발급은 회사 관리자만 가능합니다.

## API

### Auth

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/reissue`
- `POST /api/auth/logout`

### Company

- `POST /api/companies`
- `POST /api/companies/invite-codes`
- `POST /api/companies/join`
- `GET /api/companies/users`

### Case

- `POST /api/cases`
- `GET /api/cases/active`
- `GET /api/cases/{caseId}`
- `POST /api/cases/{caseId}/members`

### Document

- `POST /api/cases/{caseId}/documents`
- `GET /api/cases/{caseId}/documents`
- `GET /api/documents/{documentId}`

## 설정

기본 설정 파일은 [src/main/resources/application.yml](/mnt/c/Users/user/Desktop/backend/src/main/resources/application.yml:1)입니다.

주요 환경 변수:

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `DOCUMENT_STORAGE_ROOT`

## 실행

```bash
./gradlew bootRun
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## 테스트

```bash
./gradlew test
```

현재 통합 테스트는 다음 시나리오를 포함합니다.

- 관리자 가입 시 회사 생성
- 초대코드 가입 시 회사 연결
- 문서 업로드 후 저장/해시 처리
- 다른 회사 사용자의 문서 접근 차단

## 참고 문서

- [docs/current-architecture.md](docs/current-architecture.md)
