# Backend

Java 21, Spring Boot, Gradle 기반 백엔드 프로젝트입니다.  
이 문서는 팀원이 코드 리뷰 없이도 현재 구조, 인증 흐름, 실행 방법, 확장 포인트를 빠르게 이해할 수 있도록 정리한 온보딩 문서입니다.

## 1. 프로젝트 개요

현재 프로젝트는 아래 목적에 맞춰 최소하지만 바로 확장 가능한 구조로 구성되어 있습니다.

- 공통 응답 포맷 통일
- 공통 예외 처리 통일
- JPA 공통 엔티티 기반 마련
- JWT 기반 인증/인가 기본 흐름 구현
- Redis 기반 Refresh Token 저장 및 Access Token 블랙리스트 처리
- `User`, `Country` 최소 도메인 구성

현재 단계에서 구현된 범위는 "인증의 기반 구조"입니다.  
프로필 관리, 권한 세분화, 실제 비즈니스 API는 아직 포함되어 있지 않습니다.

## 2. 기술 스택

- Java 21
- Spring Boot
- Gradle
- MySQL
- Spring Data JPA
- Spring Security
- Redis
- JWT (`jjwt`)
- Bean Validation
- BCrypt
- Lombok

## 3. 디렉터리 구조

```text
src/main/java/com/kworkerharmony/backend
├── auth
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── AuthController.java
│   └── AuthService.java
├── country
│   ├── Country.java
│   └── CountryRepository.java
├── global
│   ├── config
│   │   ├── JpaAuditingConfig.java
│   │   └── SecurityConfig.java
│   ├── entity
│   │   └── BaseEntity.java
│   ├── exception
│   │   ├── CustomException.java
│   │   ├── ErrorCode.java
│   │   └── GlobalExceptionHandler.java
│   ├── response
│   │   └── ApiResponse.java
│   └── security
│       ├── JwtAccessDeniedHandler.java
│       ├── JwtAuthenticationEntryPoint.java
│       ├── JwtAuthenticationFilter.java
│       ├── JwtProperties.java
│       ├── JwtProvider.java
│       ├── RedisTokenRepository.java
│       ├── SecurityPaths.java
│       └── UserPrincipal.java
└── user
    ├── Role.java
    ├── User.java
    ├── UserRepository.java
    └── UserType.java
```

패키지 역할은 아래처럼 이해하면 됩니다.

- `global`
  - 프로젝트 전역에서 재사용되는 설정과 공통 정책
- `auth`
  - 회원가입, 로그인, 재발급, 로그아웃 등 인증 유스케이스
- `user`
  - 사용자 엔티티와 사용자 저장소
- `country`
  - 국가 엔티티와 국가 코드 조회 저장소

## 4. 핵심 설계 요약

### 4.1 공통 응답 구조

모든 API는 아래 형태를 기준으로 응답합니다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

실패 시에는 아래 구조를 사용합니다.

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTH_401",
    "message": "Authentication is required"
  }
}
```

관련 파일:

- `global/response/ApiResponse.java`
- `global/exception/ErrorCode.java`
- `global/exception/GlobalExceptionHandler.java`

### 4.2 공통 예외 처리

비즈니스 예외는 `CustomException`으로 던지고, `GlobalExceptionHandler`에서 공통 응답 포맷으로 변환합니다.

이 구조를 둔 이유는 다음과 같습니다.

- 컨트롤러마다 `try-catch`를 반복하지 않기 위해
- 에러 응답 포맷을 프로젝트 전체에서 동일하게 유지하기 위해
- 에러 코드를 프론트엔드와 명확히 약속하기 위해

### 4.3 BaseEntity

모든 공통 엔티티는 `BaseEntity`를 상속받습니다.

- `createdAt`
- `updatedAt`

JPA Auditing이 활성화되어 있어 엔티티 생성/수정 시 자동 반영됩니다.

관련 파일:

- `global/entity/BaseEntity.java`
- `global/config/JpaAuditingConfig.java`

## 5. 도메인 모델

### 5.1 User

`User`는 인증의 중심 엔티티입니다.

필드:

- `id`
- `email`
- `passwordHash`
- `name`
- `role`
- `userType`
- `country`
- `createdAt`
- `updatedAt`

특징:

- `email`은 unique
- 비밀번호는 원문 저장하지 않고 `passwordHash`로 저장
- `country`는 `ManyToOne`
- `role`은 현재 `USER`만 존재
- `userType`은 `WORKER`, `EMPLOYER`

### 5.2 Country

`Country`는 사용자의 소속 국가 정보를 나타냅니다.

필드:

- `id`
- `countryCode`
- `name`
- `createdAt`
- `updatedAt`

특징:

- `countryCode` 기준 조회 가능
- 회원가입 시 `countryCode`로 국가를 찾고, 해당 엔티티를 `User`에 연결

## 6. 인증 구조 전체 흐름

이 프로젝트의 인증은 크게 4개 컴포넌트로 나뉩니다.

1. `AuthController`
2. `AuthService`
3. `JwtProvider`
4. `JwtAuthenticationFilter`

흐름을 한 줄로 요약하면 아래와 같습니다.

1. 로그인 요청
2. `AuthService`가 사용자 인증
3. `JwtProvider`가 access/refresh token 발급
4. refresh token은 Redis에 저장
5. 이후 보호된 API 요청 시 `JwtAuthenticationFilter`가 access token 검사
6. 유효하면 `SecurityContext`에 인증 정보 저장

## 7. JWT 설계

### 7.1 Access Token

역할:

- 보호된 API 요청 인증

저장 위치:

- 서버 저장 없음
- 클라이언트가 `Authorization` 헤더에 담아 전달

헤더 형식:

```http
Authorization: Bearer {accessToken}
```

포함 정보:

- `sub`: 사용자 이메일
- `userId`: 사용자 ID
- `tokenType`: `ACCESS`
- `iat`
- `exp`

### 7.2 Refresh Token

역할:

- access token 재발급

저장 위치:

- Redis

포함 정보:

- `sub`: 사용자 이메일
- `userId`: 사용자 ID
- `tokenType`: `REFRESH`
- `iat`
- `exp`

### 7.3 토큰 타입을 분리한 이유

access token과 refresh token 모두 JWT이기 때문에, 타입 구분 없이 검증하면 refresh token을 access token처럼 잘못 사용하는 문제가 생길 수 있습니다.

이 프로젝트는 `tokenType` 클레임으로 아래를 강제합니다.

- access 검증 시 `ACCESS`만 허용
- refresh 검증 시 `REFRESH`만 허용
- logout에서는 만료된 access token도 타입 확인 가능

관련 파일:

- `global/security/JwtProvider.java`

## 8. Redis 토큰 관리 정책

### 8.1 Refresh Token 저장

사용자당 refresh token은 1개만 유지합니다.

키 규칙:

```text
refresh:{userId}
```

예시:

```text
refresh:1
```

의미:

- 사용자가 다시 로그인하면 기존 refresh token은 덮어쓰기
- 가장 최근 로그인 세션만 유효하게 유지

### 8.2 Access Token 블랙리스트

로그아웃한 access token은 즉시 무효화할 수 있어야 하므로 Redis 블랙리스트에 저장합니다.

키 규칙:

```text
blacklist:access:{token}
```

TTL:

- access token의 남은 유효 시간

의미:

- 토큰이 원래 만료될 때까지만 블랙리스트 유지
- 불필요하게 영구 저장하지 않음

관련 파일:

- `global/security/RedisTokenRepository.java`

## 9. Security 동작 방식

### 9.1 공개 경로

현재 `/auth/**`는 인증 없이 접근 가능합니다.

포함 API:

- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/reissue`
- `POST /auth/logout`

주의:

- 현재 `logout`도 공개 경로에 포함됩니다.
- 대신 요청 바디에 `accessToken`을 직접 받아 처리합니다.
- 향후 정책을 변경하고 싶다면 `/auth/logout`만 인증 필요 경로로 분리하면 됩니다.

### 9.2 보호 경로

`/auth/**` 외 모든 요청은 인증이 필요합니다.

### 9.3 JWT Filter

`JwtAuthenticationFilter`는 요청마다 다음 순서로 동작합니다.

1. `Authorization` 헤더 확인
2. `Bearer ` 접두사 확인
3. access token 추출
4. Redis 블랙리스트 확인
5. JWT 서명/만료/타입 검증
6. `userId` 추출
7. DB에서 사용자 조회
8. `SecurityContext`에 인증 정보 저장

토큰이 없거나 유효하지 않으면 인증 없이 다음 필터로 넘기고, 보호된 API에서 최종적으로 401 응답이 반환됩니다.

관련 파일:

- `global/config/SecurityConfig.java`
- `global/security/JwtAuthenticationFilter.java`
- `global/security/JwtAuthenticationEntryPoint.java`
- `global/security/JwtAccessDeniedHandler.java`

## 10. Auth 유스케이스 설명

### 10.1 Signup

입력:

- `email`
- `password`
- `name`
- `userType`
- `countryCode`

동작:

1. 이메일 중복 검사
2. `countryCode`로 국가 조회
3. 비밀번호 BCrypt 해싱
4. `User` 저장

특징:

- 토큰 발급 없음
- `role`은 현재 `USER`로 고정

### 10.2 Login

입력:

- `email`
- `password`

동작:

1. 이메일로 사용자 조회
2. 비밀번호 비교
3. access token 발급
4. refresh token 발급
5. Redis에 `refresh:{userId}` 저장
6. 두 토큰 반환

### 10.3 Reissue

입력:

- `refreshToken`

동작:

1. refresh token 서명/만료/타입 검증
2. token에서 `userId` 추출
3. Redis 저장값과 비교
4. 사용자 조회
5. 새 access token 발급

현재 정책:

- refresh token은 재발급 시 회전시키지 않음
- 저장된 refresh token이 일치해야만 access token 재발급 가능

### 10.4 Logout

입력:

- `accessToken`

동작:

1. 만료 허용 상태로 access token 타입 검증
2. token에서 `userId` 추출
3. Redis에서 `refresh:{userId}` 삭제
4. access token을 블랙리스트에 저장

의도:

- refresh token 재사용 차단
- 아직 만료되지 않은 access token 즉시 무효화

관련 파일:

- `auth/AuthService.java`
- `auth/AuthController.java`

## 11. API 명세

### 11.1 회원가입

`POST /auth/signup`

Request:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe",
  "userType": "WORKER",
  "countryCode": "KR"
}
```

Response:

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

### 11.2 로그인

`POST /auth/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "accessToken": "access-token",
    "refreshToken": "refresh-token"
  },
  "error": null
}
```

### 11.3 토큰 재발급

`POST /auth/reissue`

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Response:

```json
{
  "success": true,
  "data": {
    "accessToken": "new-access-token"
  },
  "error": null
}
```

### 11.4 로그아웃

`POST /auth/logout`

Request:

```json
{
  "accessToken": "access-token"
}
```

Response:

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

## 12. curl 예시

### 12.1 회원가입

```bash
curl -X POST http://localhost:8080/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "password":"password123",
    "name":"John Doe",
    "userType":"WORKER",
    "countryCode":"KR"
  }'
```

### 12.2 로그인

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"user@example.com",
    "password":"password123"
  }'
```

### 12.3 재발급

```bash
curl -X POST http://localhost:8080/auth/reissue \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken":"{refreshToken}"
  }'
```

### 12.4 로그아웃

```bash
curl -X POST http://localhost:8080/auth/logout \
  -H "Content-Type: application/json" \
  -d '{
    "accessToken":"{accessToken}"
  }'
```

## 13. 로컬 실행 방법

### 13.1 사전 준비

필수:

- Java 21
- MySQL
- Redis

권장:

- IntelliJ IDEA
- TablePlus 또는 DBeaver
- Redis CLI 또는 RedisInsight

### 13.2 환경 변수

`src/main/resources/application.yml` 기준으로 아래 값을 사용할 수 있습니다.

```yaml
spring:
  datasource:
    url: ${MYSQL_URL}
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

jwt:
  secret: ${JWT_SECRET}
  access-token-expiration-seconds: ${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS}
  refresh-token-expiration-seconds: ${JWT_REFRESH_TOKEN_EXPIRATION_SECONDS}
```

기본값:

- `MYSQL_URL=jdbc:mysql://localhost:3306/backend?serverTimezone=Asia/Seoul&characterEncoding=UTF-8`
- `MYSQL_USERNAME=root`
- `MYSQL_PASSWORD=password`
- `REDIS_HOST=localhost`
- `REDIS_PORT=6379`
- `JWT_SECRET=change-me-to-a-secure-secret-key-for-jwt-signing`
- `JWT_ACCESS_TOKEN_EXPIRATION_SECONDS=3600`
- `JWT_REFRESH_TOKEN_EXPIRATION_SECONDS=1209600`

### 13.3 애플리케이션 실행

테스트:

```bash
./gradlew test
```

실행:

```bash
./gradlew bootRun
```

## 14. DB 준비 사항

현재 JPA 설정은 `ddl-auto: none`입니다.

즉, 애플리케이션 실행 전에 실제 DB 스키마가 준비되어 있어야 합니다.  
이 프로젝트는 아직 Flyway/Liquibase나 별도 DDL 파일을 포함하지 않으므로, 로컬 실행 시에는 수동으로 테이블을 준비해야 합니다.

최소 필요 테이블 개념은 아래와 같습니다.

- `countries`
  - `id`
  - `country_code`
  - `name`
  - `created_at`
  - `updated_at`
- `users`
  - `id`
  - `email`
  - `password_hash`
  - `name`
  - `role`
  - `user_type`
  - `country_id`
  - `created_at`
  - `updated_at`

또한 회원가입을 사용하려면 `countries` 테이블에 적어도 하나 이상의 국가 데이터가 있어야 합니다.

예시:

```text
countryCode = KR
name = Korea
```

## 15. 주요 코드 진입점

처음 프로젝트를 읽는 팀원이 우선적으로 보면 좋은 파일 순서는 아래와 같습니다.

1. `global/config/SecurityConfig.java`
2. `global/security/JwtAuthenticationFilter.java`
3. `global/security/JwtProvider.java`
4. `auth/AuthController.java`
5. `auth/AuthService.java`
6. `global/exception/GlobalExceptionHandler.java`
7. `user/User.java`
8. `country/Country.java`

이 순서로 보면 보안 설정, 인증 처리, 비즈니스 흐름, 예외 정책, 도메인 구조를 빠르게 이해할 수 있습니다.

## 16. 현재 제한 사항

- 권한은 현재 `USER` 단일 값만 사용
- refresh token rotation은 구현하지 않음
- Swagger/OpenAPI 문서화 없음
- DB 마이그레이션 도구 없음
- 사용자 상세 조회/수정 API 없음
- `logout`은 access token을 요청 바디로 받는 단순 방식

이 제한 사항들은 현재 프로젝트의 목표가 "JWT 인증 베이스 구조 확립"이기 때문에 의도적으로 남겨둔 것입니다.

## 17. 다음 확장 추천

현재 구조 위에서 다음 순서로 확장하는 것이 안전합니다.

1. DB 마이그레이션 도구 도입
2. 국가/사용자 초기 데이터 전략 정리
3. 인증 사용자 주입 방식 추가
4. 사용자 프로필 API 추가
5. 권한 정책 확장
6. 통합 테스트 보강
7. Swagger/OpenAPI 추가

## 18. 참고 메모

- `jwt.secret`는 운영 환경에서 반드시 충분히 긴 안전한 값으로 교체해야 합니다.
- 테스트는 H2 기반으로 동작합니다.
- Redis는 실제 인증 플로우에서 필요하며, 컨텍스트 로딩 테스트 자체는 설정만 맞으면 통과합니다.
- 현재 문서는 코드 기준으로 작성되었으며, 구조가 바뀌면 함께 갱신해야 합니다.
