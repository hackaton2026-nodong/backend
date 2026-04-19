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

문서 업로드 완료 시 현재 확인 가능한 상태 전이는 아래와 같습니다.

- `UPLOADED`
- `STORED`
- `HASHED`

## 로컬 실행

인프라 실행:

```bash
docker compose up -d
```

백엔드 실행:

```bash
./gradlew bootRun
```

기본값은 Docker Compose 기준으로 맞춰져 있어 별도 환경변수 없이 실행 가능합니다.
로컬 MySQL 또는 다른 접속 정보를 사용할 경우 아래 환경 변수를 본인 환경에 맞게 지정하면 됩니다.

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

문서 업로드 테스트 페이지:

- `http://localhost:8080/document-upload-test.html`

## 주요 환경 변수

- `MYSQL_URL`
- `MYSQL_USERNAME`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `JWT_SECRET`
- `DOCUMENT_STORAGE_ROOT`

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

## 참고 문서

- [current-architecture.md](/mnt/c/Users/user/Desktop/backend/docs/current-architecture.md:1)
