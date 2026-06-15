# Gold Market — 담당 파트 포트폴리오 (회원 도메인)

> Spring Boot 3.5 / Java 21 / Oracle Cloud Autonomous DB / JPA / Thymeleaf / Redis / Spring Security

---

## TOP 10 핵심 기능

---

### 1. SSE 기반 플랫폼별 중복 로그인 차단

**추상적 설명**
단순한 "중복 로그인 방지"가 아니라, 같은 플랫폼(PC/Android/iOS)에서만 중복을 막는 세분화된 세션 정책을 구현했다. 기존 세션을 서버 폴링 없이 SSE(Server-Sent Events)로 실시간 알림 후 강제 종료하는 구조로, HTTP 요청 없이도 서버가 브라우저를 제어할 수 있음을 보여준다.

**구체적 설명**
- `PlatformDetector`: `User-Agent` 헤더를 파싱해 `ANDROID` / `IOS` / `PC` 3종 분류
- `SessionManagementService.register()`: 로그인 성공 시 `FindByIndexNameSessionRepository`로 Redis에서 해당 회원의 모든 세션 조회 → 동일 플랫폼 기존 세션에 `SseEmitterService.sendForceLogout()` 호출 후 `sessionRepository.deleteById()`로 삭제
- `SseEmitterService`: `ConcurrentHashMap<sessionId, SseEmitter>`로 연결 관리, 연결 종료(완료/타임아웃/오류) 시 자동 제거
- `GET /sse/connect`: 로그인 후 모든 페이지에서 자동 구독, 브라우저가 `force-logout` 이벤트 수신 시 알림 표시 후 로그아웃 처리
- `FormLoginSuccessHandler` / `OAuth2SuccessHandler` 양쪽 모두에서 `register()` 호출해 폼·소셜 로그인 동일하게 적용

---

### 2. Toss Payments 빌링키 정기결제 + 자동 갱신 스케줄러

**추상적 설명**
카드를 한 번 등록하면 매월 자동으로 결제되는 구독 서비스를 직접 구현했다. 결제 실패·재시도·상태 전이를 고려한 설계로, 단순 API 연동을 넘어 서비스 운영 수준의 안정성을 확보했다.

**구체적 설명**
- 빌링키 발급: 프론트 Toss 위젯에서 `authKey` + `customerKey` 획득 → 서버에서 Toss `POST /v1/billing/authorizations/issue` 호출 → 빌링키 DB 저장
- 첫 결제: 빌링키 발급 직후 `POST /v1/billing/{billingKey}`로 즉시 과금, 성공 시 `memberships` 저장 + `memberships_log` 이력 기록
- 상태 머신: `NONE → ACTIVE → CANCELED → EXPIRED` 단방향 전이, `ACTIVE/CANCELED` 상태에서 재가입 시도 시 예외 처리
- 스케줄러 2개: 매일 `02:00` CANCELED 만료 처리, 매일 `09:00` ACTIVE 멤버십 자동 갱신 — 건별 `try-catch` 격리로 한 건 실패가 전체 갱신 중단 방지
- 취소: 즉시 해지 아닌 `scheduleCancel()` — 만료일까지 혜택 유지 후 스케줄러가 EXPIRED 전환

---

### 3. Google OAuth2 / OIDC 소셜 로그인 + 인증 객체 통일

**추상적 설명**
소셜 로그인 신규 가입 시 일반 가입과 동일한 부가 처리(멤버십 생성, 쿠폰 발급)를 빠뜨리지 않고 트랜잭션 내에서 원자적으로 처리했다. 폼 로그인과 소셜 로그인의 인증 객체를 통일해 이후 컨트롤러에서 분기 없이 사용할 수 있게 설계했다.

**구체적 설명**
- `CustomOAuth2UserService`가 `OidcUserService`를 확장 — Google ID Token 검증 후 자체 DB와 매핑
- `orElseGet()` 블록(신규 가입 경로) 안에서 `Member` 저장 → `Memberships`(STATUS_NONE) 저장 → `MemberCoupon`(웰컴 쿠폰) 저장을 한 트랜잭션으로 처리
- `CustomUserDetails`가 `UserDetails`와 `OidcUser` 동시 구현 → `@AuthenticationPrincipal CustomUserDetails`로 폼·소셜 로그인 구분 없이 동일하게 주입

---

### 4. 비로그인 장바구니(localStorage) + 로그인 시 자동 병합

**추상적 설명**
로그인을 강제하지 않고 비회원도 장바구니를 쓸 수 있게 해 구매 이탈을 줄이고, 로그인하는 순간 로컬 데이터를 서버로 자동 병합하는 흐름을 구현했다. 서버 수정 없이 프론트 공통 레이아웃 한 곳에서 병합 로직을 처리해 모든 페이지에서 동작한다.

**구체적 설명**
- 상품 상세 페이지: `POST /api/cart` 401 응답 시 리다이렉트 대신 `localStorage.guestCart`에 `{optionsSeq, quantity, productName, price, thumbnailUrl}` 저장
- 장바구니 페이지: `guestMode=true` 모델로 Thymeleaf 분기, JS가 `localStorage` 데이터 읽어 동일한 UI 렌더링
- `layout.html` 공통 스크립트: 페이지 로드 시 인증 상태 확인 → `guestCart` 존재하면 `POST /api/cart` 반복 호출로 서버 병합 → 완료 후 `localStorage` 초기화
- 병합 시 `CartLogService.logAdd()`도 호출해 비로그인 담기 → 로그인 병합 경로도 이력 기록

---

### 5. 이메일 기반 비밀번호 재설정 (UUID 토큰)

**추상적 설명**
비밀번호를 잊었을 때 이메일로 본인 확인 후 재설정하는 흐름을 직접 구현했다. 토큰 유효 시간과 일회성 사용을 엔티티 레벨에서 관리해 재사용·탈취 공격에 안전한 구조를 갖췄다.

**구체적 설명**
- `EmailToken` 엔티티: UUID 토큰 + `expireAt`(`LocalDateTime`) + `used(boolean)` — 만료 시각과 사용 여부를 DB에서 관리
- 발급: `PasswordResetService.sendResetEmail()` → UUID 생성 → `EmailToken` 저장 → `JavaMailSender`로 링크 이메일 발송
- 검증: `GET /member/reset-password?token=` 접근 시 `expireAt` 초과 또는 `used=true`이면 오류 페이지
- 완료: 새 비밀번호 `BCrypt` 인코딩 후 `Member.updatePassword()` 호출(dirty checking) → `token.used = true`로 즉시 만료

---

### 6. Oracle Cloud Autonomous DB TCPS / Wallet 연결

**추상적 설명**
로컬 DB가 아닌 실제 클라우드 DB(Oracle Cloud Free Tier)에 프로젝트를 연결했다. 암호화 연결(TCPS)과 팀 전체가 접속 가능한 공유 환경을 구성하면서 발생한 인프라 문제를 해결한 경험이다.

**구체적 설명**
- Oracle Wallet(ZIP) 기반 TCPS 연결 — `ojdbc.properties`에 `wallet_location` 경로 지정
- `ORA-17957 SSO KeyStore not available` 오류: `oraclepki` / `osdt_cert` / `osdt_core` 의존성 누락이 원인 → `build.gradle`에 추가로 해결
- `HikariCP maximum-pool-size: 2` 의도적 설정 — Free Tier 최대 동시 접속 10개 제한을 고려해 팀 전체 합산 초과 방지
- `application-secret.yml` 분리 + `.gitignore` 처리 — DB 비밀번호, OAuth2 시크릿, Cloudinary 키 등 민감 정보를 코드와 완전 분리

---

### 7. Redis 세션 클러스터링 + 인덱스 조회

**추상적 설명**
Spring의 기본 인메모리 세션 대신 Redis에 세션을 저장해 서버 재시작 시에도 로그인이 유지되게 했다. 단순 저장을 넘어 회원 ID로 세션을 역조회하는 인덱스 기능을 활용해 "이 회원의 모든 세션"을 서버 측에서 찾을 수 있는 구조를 구축했다.

**구체적 설명**
- `@EnableRedisIndexedHttpSession` — 세션을 Redis에 저장 + `principal.name` 기준 인덱스 자동 생성
- `FindByIndexNameSessionRepository.findByPrincipalName(username)` — SSE 강제 로그아웃, 관리자 강제 로그아웃 시 활용
- `FormLoginSuccessHandler`에서 `session.setAttribute(PRINCIPAL_NAME_INDEX_NAME, username)` 저장해 인덱스 연동
- `RedisConfig`에 `@Bean ObjectMapper` 등록 — 세션 직렬화 시 타입 정보 보존

---

### 8. 회원가입 3단계 플로우 + 세션 기반 상태 전달

**추상적 설명**
여러 페이지에 걸친 가입 플로우에서 단계 간 데이터를 어떻게 전달할지 설계했다. URL 파라미터(노출 위험)와 숨김 필드(누락 위험) 대신 서버 세션을 선택해 데이터 신뢰성과 보안을 확보했다.

**구체적 설명**
- 플로우: 유형 선택(`/signup/type`) → 약관 동의(`/signup/terms`) → 정보 입력(`/signup`)
- 약관 동의 POST: `signupType`(일반/소셜), `marketing`(boolean, 미체크 시 `defaultValue="false"`) 세션 저장 후 redirect
- 최종 가입 처리: 세션에서 값 꺼내 `notification_preferences` 테이블에 마케팅 수신 동의 저장
- `marketing` 파라미터를 `@RequestParam(defaultValue="false")`로 처리 — 체크박스 미체크 시 서버에 파라미터 자체가 오지 않는 HTML 특성 대응

---

### 9. 기본 배송지 단일 유지 — 서비스 레벨 제약

**추상적 설명**
"기본 배송지는 항상 하나"라는 비즈니스 규칙을 DB UNIQUE 제약이 아닌 서비스 레이어에서 처리했다. JPA dirty checking을 활용해 불필요한 `save()` 호출 없이 상태를 변경하는 방식도 적용했다.

**구체적 설명**
- `DeliveryAddress` 엔티티에 `setDefault()` / `clearDefault()` 의도 명확한 메서드 정의 (setter 없음)
- 기본 배송지 변경 시: `findByMember_Seq()`로 전체 조회 → `defaultYn == "Y"`인 것 `clearDefault()` → 신규 항목 `setDefault()` → `@Transactional` 안에서 dirty checking으로 자동 UPDATE (별도 `save()` 불필요)
- DB에 소문자 `'y'`로 저장된 레거시 데이터 대응: Thymeleaf `#strings.toUpperCase()` + `UPDATE SET default_yn = UPPER(default_yn)` 병행 처리
- `findByMember_SeqOrderByDefaultYnDesc()` — 기본 배송지가 항상 목록 최상단

---

### 10. 장바구니 감사 로그 (Cart Audit Log)

**추상적 설명**
장바구니 담기·삭제·구매 이벤트를 별도 로그 테이블에 기록해 운영 분석과 문제 추적이 가능한 구조를 만들었다. 로그인 회원만 기록하되, 비로그인 카트가 로그인 후 병합되는 경로도 누락 없이 기록하는 경계 조건을 처리했다.

**구체적 설명**
- `CartLog` 엔티티: `member(FK)`, `options(FK)`, `status(VARCHAR2)`, `actionDate(LocalDateTime)` — DDL의 `TIMESTAMP` 타입에 맞춰 `LocalDateTime` 사용
- 상수: `STATUS_ADD = "0"` / `STATUS_REMOVE = "1"` / `STATUS_PURCHASE = "2"` — 매직 스트링 제거
- `CartLogService.logAdd()` / `logRemove()` / `logPurchase()` 메서드를 `CartService` 각 지점에서 직접 호출 — AOP 미사용, 도메인 맥락(어느 옵션인지, 어느 회원인지)을 서비스 레이어에서 자연스럽게 확보
- 병합 경로 처리: `layout.html` 병합 스크립트 → `POST /api/cart` → `CartService.addCart()` → `logAdd()` 호출 — 비로그인 담기도 병합 시점에 로그 생성

---

## 기술 스택 요약

| 분류 | 기술 |
|---|---|
| Backend | Spring Boot 3.5, Java 21, Spring Security, Spring Session |
| ORM | JPA / Hibernate |
| DB | Oracle Cloud Autonomous DB (TCPS/Wallet), Redis |
| 결제 | Toss Payments v2 (빌링키 정기결제) |
| 외부 API | Google OAuth2 / OIDC, Kakao 우편번호 API, Cloudinary |
| 실시간 | SSE (Server-Sent Events) |
| Frontend | Thymeleaf, Tailwind CSS, Vanilla JS / Fetch API |
