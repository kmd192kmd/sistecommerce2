# Gold Market — 담당 개발 기능 정리

> Spring Boot 3.5 / Java 21 / Oracle DB / JPA / Thymeleaf / Redis

---

## 1. 회원 인증 부가 기능

### 아이디 찾기
- 이름 + 이메일 일치 확인 후 마스킹된 아이디 반환
- `FindUsernameService` / `FindUsernameController` / `find-username.html`

### 이메일 기반 비밀번호 재설정
- UUID 토큰 발급 → 이메일 발송 → 토큰 검증 → 비밀번호 변경 3단계 플로우
- `EmailToken` 엔티티로 유효 시간 관리, 사용 후 즉시 만료 처리
- `EmailService` / `PasswordResetService` / `find-password.html` / `reset-password.html`

---

## 2. 로그인 로그

- 폼 로그인 성공·실패, Google 소셜 로그인 성공을 각 핸들러에서 `login_log` 테이블에 기록
- 기록 항목: IP, User-Agent, 로그인 방식, 성공/실패 여부, 일시
- `LoginLog` 엔티티 / `LoginLogService`
- `FormLoginSuccessHandler`, `FormLoginFailureHandler`, `OAuth2SuccessHandler` 수정

---

## 3. 소셜 로그인 신규 가입 연동 (Google OAuth2 / OIDC)

- `OidcUserService` 확장(`CustomOAuth2UserService`) — Google ID Token 검증 후 자체 DB 회원 연동
- 기존 회원이면 조회, **신규 회원이면 일반 가입과 동일하게 자동 처리**
  - `memberships` 레코드 생성 (STATUS_NONE)
  - 웰컴 쿠폰 자동 발급 (`member_coupon` INSERT)
- `CustomUserDetails`가 `OidcUser`를 구현해 폼 로그인·소셜 로그인 인증 객체 통일

---

## 4. 멤버십 (정기 구독)

- **Toss Payments 빌링키 방식** — 카드 등록(authKey) → 빌링키 발급 API 호출 → 즉시 첫 결제 → DB 저장
- 구독 상태 흐름: `NONE → ACTIVE → CANCELED → EXPIRED`
- 취소 시 만료일까지 혜택 유지 후 자동 만료 (`scheduleCancel`)
- **스케줄러 2개** (`@Scheduled`)
  - 매일 `02:00` — CANCELED 상태에서 만료일 초과한 멤버십 EXPIRED 전환
  - 매일 `09:00` — ACTIVE 멤버십 `nextBillingAt` 도래 시 Toss 빌링 API 자동 갱신
- 멤버십 이력 테이블(`memberships_log`) 기록 — 가입/취소/갱신/만료 이력 보존
- `MembershipService` / `MembershipController` / `MembershipScheduler` / `membership.html`

---

## 5. 플랫폼별 중복 로그인 차단 (SSE)

- 로그인 시 `User-Agent`로 접속 플랫폼 감지 — `ANDROID` / `IOS` / `PC` 3종 구분 (`PlatformDetector`)
- **동일 플랫폼에서 재로그인 시 기존 세션 강제 종료**
  1. Redis에서 해당 회원의 모든 세션 조회 (`FindByIndexNameSessionRepository`)
  2. 동일 플랫폼 기존 세션에 SSE `force-logout` 이벤트 전송 → 브라우저에서 알림 후 자동 로그아웃
  3. 해당 세션 Redis에서 삭제
  4. 현재 세션에 플랫폼 attribute 저장
- `SessionManagementService` — 위 플로우 전담, `FormLoginSuccessHandler` / `OAuth2SuccessHandler`에서 호출
- `SseEmitterService` — `sessionId → SseEmitter` 맵(`ConcurrentHashMap`)으로 연결 관리, 연결 종료 시 자동 제거
- `GET /sse/connect` — 로그인 후 페이지 로드 시 브라우저가 SSE 구독, 세션 ID 기준으로 emitter 등록

---

## 6. 장바구니

### 비로그인 로컬 카트
- 로그인 없이 상품 상세에서 옵션 선택 후 `localStorage`에 카트 저장
- 장바구니 페이지에서 로컬 데이터 렌더링, 수량 변경·삭제·금액 계산 모두 클라이언트 처리

### 로그인 시 자동 병합
- `layout.html` 공통 스크립트 프래그먼트에서 로그인 상태 감지
- 로컬 카트 항목을 `POST /api/cart`로 일괄 서버 저장 후 `localStorage` 초기화

### 서버 카트 기능
- 수량 변경 `PATCH /api/cart/{seq}` — 재고 초과 시 서버에서 예외
- 항목 삭제 `DELETE /api/cart/{seq}` — 본인 소유 확인 후 삭제
- 전체 금액 실시간 계산 (옵션 추가금 포함)
- 재고 초과 방지 서버·클라이언트 이중 검증

### 장바구니 로그
- `cart_log` 테이블에 담기(0), 삭제(1), 구매(2) 상태 기록
- 로그인 회원만 기록, 비로그인 카트 병합 시에도 담기 로그 생성
- `CartLogService` — `logAdd()` / `logRemove()` / `logPurchase()`

---

## 6. 마이페이지

### 쿠폰
- 보유 쿠폰 미사용/사용완료 탭 분리
- 할인 유형(금액/비율), 유효기간 표시
- `MemberCouponService` / `coupons.html`

### 개인정보 확인·수정
- 닉네임·전화번호·주소 수정
- `MyProfileService` / `profile.html`

### 리뷰 관리
- 내가 작성한 리뷰 목록 조회
- `MyReviewService` / `reviews.html`

### 공동구매 내역
- 참여한 공동구매 내역 최신순 조회
- 참여 상태별 뱃지 색상 구분 (결제대기=yellow, 참여중=green, 확정=blue, 취소/만료=gray, 실패=red)
- `MyGroupBuyDto` / `MyGroupBuyService` / `MyGroupBuyController` / `groupbuy.html`

---

## 설계 포인트

- **소셜·폼 인증 통일** — `CustomUserDetails`가 `OidcUser` 구현으로 Spring Security 인증 객체 단일화
- **로그 도메인 분리** — 로그인 로그(핸들러), 장바구니 로그(서비스 레이어)를 각 도메인에서 직접 기록해 AOP 남용 없이 도메인 맥락 보존
- **비로그인 UX** — 로그인 강제 없이 장바구니 사용 가능, 로그인 직후 자동 병합으로 이탈 없는 구매 플로우 구성
- **결제 안전성** — ACTIVE/CANCELED 상태 재가입 차단, 스케줄러 장애 격리(건별 try-catch)로 한 건 실패가 전체 갱신을 막지 않음
- **서비스 분리** — `MyGroupBuyService`(조회 전용)를 `GroupBuyService`(참여·마감 처리)에서 분리해 단일책임 원칙 준수
- **플랫폼별 세션 격리** — PC/Android/iOS를 독립 세션으로 관리, 동일 플랫폼 재로그인 시 SSE로 기존 세션에 즉시 알림 후 강제 종료 (서버 폴링 없이 실시간 처리)
