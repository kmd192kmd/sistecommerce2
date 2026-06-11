# AGENTS.md

> Codex가 이 프로젝트에서 작업할 때 따르는 가이드.
> 작업 시작 전 아래 **핵심 참고 문서**를 먼저 읽을 것.
> 이 파일과 문서/DDL이 충돌하면 **문서/DDL을 우선**한다 (single source of truth).

---

## 프로젝트 개요

스프링 부트 기반 **공동구매(공구) + 종합 쇼핑몰** E-Commerce 플랫폼.
일반 쇼핑몰 기능을 갖추되, **공동구매 메커니즘이 핵심 차별점**이다. (종합몰)

공구 = 정해진 기간 안에 최소 인원 이상 모이면 **고정 할인가**로 구매. 미달 시 전원 무산(전액 환불).

---

## 기술 스택

- **Backend**: Spring Boot, Spring MVC
- **Security**: Spring Security (필수 요구사항)
- **AI**: Spring AI (필수 요구사항) — **pgvector 기반 벡터 유사도 상품 추천** 방향
- **DB**: Oracle (DDL은 Oracle 문법 기준)
- **결제**: PG 연동 (payment_method에 토스페이먼츠 빌링키 사용 흔적 있음)
- **이미지**: Cloudinary (product_image / review_image의 public_id)
- **인프라**: Docker / docker-compose

> 메인 DB는 **Oracle**(DDL 기준). Spring AI용 벡터 스토어(pgvector 등) 스키마는 DDL에 아직 없으므로, Spring AI 추천 기능 구현 단계에서 추가/확정한다.

---

## 핵심 참고 문서 (작업 전 필독)

레포 `docs/` 에 두고 항상 참조한다.

1. `docs/요구사항_분석서.md` — 공구/회원/관리자/시스템 기능 + 비기능 요구사항(NFR)
2. `docs/데이터_설계_지적사안.md` — 일반/공구 주문 구분, 쿠폰 차단, 흐름별 처리(참여~발주~배송~환불)
3. `docs/DDL.sql` — 전체 테이블/제약/FK (가장 최신: 260601 16시). **컬럼 의미는 DDL의 COMMENT 참조**
4. `docs/통합_기능_체크리스트.csv` — 전체 기능 목록 + 우선순위 + 담당자

---

## 핵심 아키텍처 결정사항 (반드시 지킬 것)

### 공구 결제 모델
- **고정 할인가** (`group_buy.final_price`). 참여 = 즉시 PG 결제. 진행 중 정가 변동 대비 `original_price` 별도 보관.
- 마감 시 **확정 인원(결제 완료자) ≥ `min_count` → 확정**, 미달 → **전원 일괄 결제취소(무산)**.
- 1인당 1상품, 수량 1개 고정.

### 주문 구조
- 공구 주문 식별: `order_item.participation_seq` 가 NULL이 아니면 공구, NULL이면 일반 주문.
- `participation` 은 `orders` 가 아니라 **`order_item` 에 연결**한다. (orders는 여러 상품 묶음일 수 있으나 공구는 1인 1상품이라서)

### 쿠폰/핫딜 차단 (공구 한정)
- 공구 상품엔 쿠폰·핫딜 **적용 불가**.
- 서비스 로직 + **DB 체크 제약으로 이중 방어**: `CK_groupbuy_no_discount`
  (`participation_seq` 가 NULL이 아니면 `coupon_discount = 0 AND hotdeal_discount = 0`).

### 옵션 / 재고 점유
- 옵션별 발주가능수량 `group_buy_option.order_qty`. **옵션별 order_qty 합 = `max_count`**.
- 점유 관리: `group_buy_option.occupied_count` = 확정 인원 + 결제대기 인원. **매진 판단 기준**.
- 점유(차감) 시점: 정규 참여 결제 시 / 대기열 승격(결제대기 진입) 시. 취소·미결제 시 복구.
- 회원은 옵션의 **매진 여부만** 조회 가능, 옵션별 잔여 수량은 비공개. 참여 후 옵션 변경 불가.

### 대기열 / 승격 (`waiting_queue`)
- `max_count` 도달 또는 특정 옵션 매진 시 추가 신청은 대기열로.
- 정규 참여자 이탈(취소/결제기한 만료) 시 **같은 옵션** 대기열의 **FIFO(`created_at`) 첫 사용자**를 자동 승격.
- 승격자 결제기한 `participation.payment_deadline` = **min(승격시각 + 24h, 공구 마감시각)**.
- 승격~결제대기 시점엔 `orders`/`payment` 행을 만들지 않음. **결제 완료 시 한 트랜잭션으로 생성**.

### 시간 정책 불변조건(Invariant)
- 마감 `T_lock`(현재 24h) 전부터 **참여 취소 불가**.
- 불변조건: **`T_lock >= T_pay`** (T_pay = 승격자 결제 제한시간, 현재 24h).
  깨지면 승격자 결제기한이 마감 이후로 밀려 마감 정합성이 붕괴됨.

### participation.status
`PAYMENT_PENDING`(결제대기) → `PARTICIPATING`(참여중) → `CONFIRMED`(확정) / `CANCELLED`(취소) / `FAILED`(무산)

### group_buy.status
`SCHEDULED` → `ONGOING` → `CONFIRMED`(확정마감) / `FAILED`(무산) / `STOPPED`(관리자 강제중단)

---

## 비기능 요구사항 (NFR) — 동시성 핵심

| ID | 내용 |
|----|------|
| NFR-001 동시성 | 점유 인원(확정+결제대기) ≤ `max_count`, 옵션별로는 ≤ `order_qty`. 절대 초과 금지 |
| NFR-002 공정성 | 대기열 승격은 옵션별 FIFO 보장 |
| NFR-003 정합성 | 임의 시점에 확정 인원 = 결제 완료 건수 |
| NFR-004 멱등성 | 동일 취소 요청이 중복돼도 PG 환불은 1회만 |
| NFR-005 신뢰성 | 취소 → 승격 → 결제는 단일 트랜잭션 또는 saga 패턴 |
| NFR-006 관찰가능성 | 모든 결제/취소 이벤트는 로그로 기록 |

> 동시성 락 전략은 구현 단계에서 별도 논의. `occupied_count` 갱신 시 경쟁 조건 주의.

---

## DB 컨벤션
- 테이블/컬럼명: 영어 snake_case, **쌍따옴표로 감쌈** (Oracle DDL 스타일을 따를 것).
- 상태값은 대부분 숫자 코드 + DDL COMMENT에 의미 명시 (`order_status`, `payment_status`, `item_status` 등).
  **숫자 하드코딩 금지, 의미는 반드시 DDL COMMENT 확인**.
- 금액 스냅샷 컬럼(order_item의 가격류)은 구매 시점 값 보존용 — 사후 변경 금지.

---

## 개발 환경
- OS: Windows / PowerShell
- IDE: VS Code
- Docker Desktop (WSL 백엔드)

---

## 작성하지 말 것 / 주의
- 검증 안 된 추측으로 비즈니스 규칙을 만들지 말 것 — 모호하면 문서를 확인하거나 질문할 것.
- 공구에 쿠폰/핫딜을 끼워넣지 말 것 (DB가 막음).
- **옵션 스키마**: 최신 DDL의 `option` 테이블 **flat columns 구조**(color, size, volume_weight, taste, ... + stock / safety_stock / additional_price)가 확정안. row-based variant 모델은 채택하지 않음. 옵션 관련 코드는 이 구조를 기준으로 작성할 것.
