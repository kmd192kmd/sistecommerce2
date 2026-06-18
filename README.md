# 🪙 Gold Market (골드 마켓)
> **공동구매와 핫딜을 곁들인 Spring Boot 기반 E-Commerce 플랫폼**

Gold Market은 실시간 공동구매 대기열 관리, 타임 세일 핫딜, 1:1 실시간 CS 상담 및 AI 챗봇 라우팅 등 이커머스의 핵심 도메인을 고도화하고, 대규모 트래픽 하에서의 동시성 제어 및 대용량 데이터 조회 성능 개선에 집중하여 설계된 웹 애플리케이션입니다.

---

## 📅 프로젝트 개요
* **개발 기간**: 2026.05.19 ~ 2026.06.17 (약 4주)
* **목표**: 공동구매 선착순 대기열 및 재고 정합성을 완벽하게 보장하고, 분산 환경에서의 조회 오프로딩을 실측 검증한 고성능 쇼핑몰 플랫폼 구축

---

## 🛠️ 기술 스택
### Back-end
* **Core**: Java 21, Spring Boot 3.x, Spring Security
* **Message Broker**: Apache Kafka
* **Data Access**: Spring Data JPA (Hibernate), QueryDSL
* **Database**: Oracle Cloud Autonomous Database, Redis (Spring Cache)
* **Search Engine**: Elasticsearch (Nori Analyzer)
* **Build Tool**: Gradle

### Front-end
* **View Engine**: Thymeleaf (Progressive Enhancement 기반 SPA-like 인터랙션)
* **Component-based UI**: React (대시보드 통계 차트, 배송 현황 모달 등 부분 도입)
* **Styling**: Tailwind CSS

### Infra & DevOps
* **Containerization**: Docker Compose (Redis, Elasticsearch, Kafka)
* **CI/CD**: GitHub Actions (AWS EC2 배포 파이프라인 자동화)
* **Third Party APIs**: Toss Payments (결제/환불), Kakao Mobility API (배송 경로), 한국천문연구원 특일 정보 API (공휴일 배송 제외 연산)

---

## 👥 팀원 소개 및 담당 역할
| 이름 | 담당 역할 및 핵심 구현 사항 |
| :--- | :--- |
| **김민도** | **쇼핑몰 목록, 주문/반품 목록, 배송 조회 구현**<br>• Elasticsearch 연동 및 검색엔진 기반 정렬/페이징 최적화 (CQRS)<br>• React + Tailwind CSS 이식 및 컴포넌트 기반 UI 개발<br>• GitHub Actions 기반 CI/CD 파이프라인 구축 |
| **박지명** | **회원 관리 & Spring Security 권한 제어**<br>• 회원가입, 로그인, 배송지 관리 시스템 개발<br>• 장바구니 상품 관리 로직 구현 및 멤버십/쿠폰 혜택 처리 |
| **곽정도** | **주문 생성 및 결제/환불 시스템 연동**<br>• 토스페이먼츠(Toss Payments) API 연동 카드/간편 결제 및 환불 구현<br>• 실물 대금이 오가는 결제 내역 관리 및 예외 롤백 처리 |
| **이세빈** | **상품 정보 관리 및 리뷰 시스템**<br>• 상품 등록/수정/삭제(CRUD), 옵션 및 판매처 매핑<br>• 상품별 리뷰 작성, 평점 및 별점 시스템 구축 |
| **이찬희** | **관리자 통계 및 CS 어드민**<br>• 최고 관리자용 메인 매출 통계 대시보드 구축<br>• 재고/발주 관리자 기능 개발 및 재고 변동 이력 추적 시스템 구현<br>• CS 관리자용 공동구매 제어 대시보드 개발 |
| **홍태훈** | **프로모션 및 회계 관리**<br>• 매출 통계 다차원 집계 쿼리 최적화 및 N+1 문제 해결<br>• 핫딜 관리, 쿠폰 발행/관리 및 회계 정산 시스템 구축 (BigDecimal 사용)<br>• 대량 쿠폰 발급용 JDBC Batch Insert 최적화 |
| **임주현** | **실시간 하이브리드 CS 시스템**<br>• WebSocket & STOMP 기반 실시간 1:1 고객 상담 대시보드 개발<br>• OpenAI API 연동 고객 지원 AI 챗봇 구현 및 상담사 라우팅 |
| **황윤재** | **공동구매 핵심 도메인 설계 및 동시성 제어**<br>• 공동구매 생애주기 관리 및 FIFO 선착순 대기열 자동 승격 구현<br>• Redis Rate Limit을 활용한 사재기 및 재고 선점(Inventory-Hold) 공격 차단<br>• SSE(Server-Sent Events) 활용 실시간 알림 기능 개발 |

---

## 🚀 핵심 개선 사항 및 문제 해결 (Troubleshooting)

### 1. 대규모 트래픽 동시성 제어 및 커넥션 풀 고갈 방지 (Redis + DB 비관적 락)
* **문제 상황**: 플래시 세일(예: 공동구매 선착순 3,000명 제한) 시 순간적으로 수만 명의 사용자가 진입하면 데이터베이스 커넥션이 고갈(Connection Pool Starvation)되어 전체 웹사이트가 다운되는 리스크 존재.
* **해결 방법**: 
  * **1차 방어선 (Redis 입장 게이트)**: 앞단 인메모리(Redis)의 원자적 카운터(Atomic Counter)를 활용해 대기열 버퍼를 초과하는 트래픽은 DB에 도달하기 전 `HTTP 429`로 즉시 거름.
  * **2차 방어선 (DB 비관적 락)**: 필터를 통과한 소수의 경쟁 상태 트래픽에 대해서는 RDBMS 수준의 비관적 락(`Pessimistic Lock`)을 걸어 한정 수량이 정확히 소진되도록 물리적 데이터 무결성 보장. (정원 초과 판매 0건 검증)

### 2. 조회 성능 극대화 및 검색 오프로딩 (Elasticsearch 이관 & CQRS)
* **문제 상황**: 다양한 필터 조건(가격대, 평점, 상태값)과 다차원 정렬(인기도 등)을 반영한 RDBMS dynamic 쿼리 실행 시, 인덱스 누락으로 인한 Full Table Scan 및 디스크 정렬(Filesort) 병목이 발생하여 평균 응답 속도가 250ms까지 저하됨.
* **해결 방법**:
  * **CQRS 패턴 적용**: 읽기(조회/검색) 연산을 Elasticsearch로 100% 이관하여 RDBMS 부하를 제로화.
  * **전문 검색 고도화**: Nori 형태소 분석기를 도입하여 초성 검색, 자동완성, 오타보정(Fuzzy) 검색 지원.
  * **정렬 오버헤드 해결**: 복잡한 인기도 산출 식을 ES의 **Painless Script Score**로 튜닝하여 세그먼트 스캔 단계에서 정렬 점수를 계산하도록 처리, Filesort 부하 차단.
  * **비동기 색인**: RDBMS CUD 발생 시 `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`와 `@Async`를 조합하여 DB 커밋 이후 백그라운드에서 ES 인덱스 색인이 비동기 동기화되도록 결합도를 격리. (평균 검색 응답 속도 **250ms ➡️ 5~10ms 대**로 단축)

### 3. 메인 추천 상품 캐싱 및 직렬화 이슈 (Redis Cache & Cache Warming)
* **문제 상황**: 메인 페이지 접속 시 계산되는 추천/인기 상품 집계 쿼리가 높은 트래픽 부하를 유발하고, Redis 캐싱 적용 시 `SerializationException` 예외가 발생함.
* **해결 방법**:
  * 메인 페이지의 조회 속도를 개선하고자 Spring Cache를 연동하여 **Redis 인메모리 캐싱**을 도입.
  * 웹 애플리케이션 시작 시점에 `@PostConstruct`를 통한 **캐시 워밍(Cache Warming)**을 실행하고, `@Scheduled` 스케줄러를 도입하여 1시간 주기로 정각마다 백그라운드에서 캐시를 갱신하도록 구성. (DB 조회 부하를 메모리 조회 0.5ms 이하로 최적화)
  * 캐시 대상 데이터인 `ProductDto`, `ProductImageDto`, `ProductOptionDto` 객체에 `java.io.Serializable` 인터페이스 구현 및 `serialVersionUID`를 설계하여 이진 직렬화 충돌 문제 해결.

### 4. 주문 취소 및 배송 트랜잭션의 무결성 보장 (Row Lock & API 멱등성)
* **문제 상황**: 사용자가 취소 처리가 늦어질 때 새로고침/뒤로가기를 연타하여 중복 환불이 요청되거나, 배송 상태 변동 배치 작업과 취소 요청이 동시에 발생해 "배송이 시작된 상품이 결제 취소"되는 데이터 불일치 가능성 존재.
* **해결 방법**:
  * **프론트엔드 제어**: 버튼 클릭 시 진행률 오버레이 모달을 덮어 연타를 차단.
  * **백엔드 DB Row Lock**: 동일 주문 데이터 수정 시 DBMS 수준에서 행 잠금을 획득하게 하여 후속 요청은 대기하도록 한 후, 트랜잭션 진행 시점의 상태(`READY`)를 재검증하여 초과 요청을 롤백 처리.
  * **결제 멱등성**: 토스페이먼츠 API 측의 `paymentKey` 기반 중복 취소 에러(`ALREADY_CANCELED_PAYMENT`) 감지 대응.
  * **배송 연동 경쟁 제어**: 배송 상태값 업데이트 시점에도 비관적 락을 획득하게 하여, 결제 취소가 배송 상태 변경 배치보다 우선 시도될 경우 배송 데이터를 취소(`FAILED`)로 강제 전환하도록 우선권 조율.

### 5. 사용자 경험 중심의 비동기 SPA 구현 및 뒤로가기 동기화 (History API)
* **문제 상황**: 페이지 전환 시 새로고침 깜빡임으로 인한 UX 저하가 발생하고, 상품 상세 페이지 조회 후 브라우저 뒤로가기 시 이전에 로컬 세션에 누적되었던 "최근 조회 상품" 목록이 화면에 갱신되지 않고 그대로 캐시되어 남아 있는 현상 발생.
* **해결 방법**:
  * **뒤로가기 부분 갱신**: 전체 강제 새로고침 방식 대신, 최근 조회 상품 목록만 Thymeleaf Fragment 조각으로 리턴하는 전용 API(`/products/recent-viewed`)를 백엔드에 개설하고, 프론트엔드에서 브라우저 뒤로가기(`pageshow` 이벤트) 감지 시 해당 영역만 AJAX로 가볍게 갱신 처리.
  * **SPA-like 렌더링**: 검색 엔진 최적화(SEO) 및 마우스 휠 클릭 기능을 보존하기 위해 기본 `<a>` 태그와 `<form>`의 구조는 유지하되, JS `e.preventDefault()`로 이벤트를 가로챈 뒤 `fetch` API를 사용하여 본문 HTML 조각만 동적으로 치환(`container.outerHTML`).
  * **History API 활용**: 페이지 전환 없이 주소창의 쿼리 스트링만 동기화하기 위해 `history.pushState` 및 `popstate` 이벤트 처리 구축.
  * **이벤트 재바인딩**: DOM이 dynamic하게 교체되면서 날아간 프론트엔드 요소에 대해 HTML 삽입 직후 `initPriceSlider()` 및 `lucide.createIcons()` 등의 스크립트를 즉시 재실행하여 이벤트 리스너 복구.

### 6. 금융 데이터의 정밀 연산 및 대량 벌크 처리 최적화
* **문제 상황**: 판매처 대금 정산 시 부동 소수점 누락으로 인한 금액 오차가 우려되고, 다수의 회계 관리자가 동일 정산 건을 승인할 때 이중 송금 리스크가 발생함. 또한 대량 회원 대상 쿠폰 일괄 발급 시 메모리 및 쿼리 병목 발생.
* **해결 방법**:
  * **소수점 정밀 연산**: 금융 산출식에서 `float`/`double` 자료형 대신 Java 전용 `BigDecimal` 객체를 채택해 연산 정합성을 100% 만족시킴.
  * **격리 수준 튜닝**: 정산 지급 과정 중 예외가 터지더라도 개별 대금 내역의 상태 롤백 유효 범위를 안전하게 가두기 위해 트랜잭션 전파 속성을 `@Transactional(propagation = Propagation.REQUIRES_NEW)`로 설정.
  * **이중 정산 방어**: DB 낙관적 락(`Optimistic Lock`)을 도입하여 다수 어드민의 동시 승인 시 한 명만 성공하도록 구현.
  * **벌크 쿼리 최적화**: 수천 명 대상 쿠폰 일괄 발급 시 싱글 인서트 루프의 성능 저하를 해결하기 위해 `JDBC Batch Insert` 방식을 연동하여 벌크 쿼리 단축 및 JPA Dirty Checking 최적화 적용.

### 7. 인프라 리소스 다이어트와 AWS 배포 환경의 메모리 확장
* **문제 상황**: 물리 RAM 2GB인 AWS EC2 Small 배포 환경에서 Spring Boot, Elasticsearch, Jenkins, Kibana, Redis 등을 동시에 구동하면 OOM(Out Of Memory)으로 인해 인프라가 주기적으로 다운됨.
* **해결 방법**:
  * **리소스 다이어트**: 상시 800MB 이상 메모리를 점유하며 빌드 시 부하를 주던 젠킨스(Jenkins) 컨테이너를 비활성화하고 GitHub Actions 파이프라인으로 전환. Kibana 비활성화 및 Elasticsearch JVM 메모리 강제 제한(`ES_JAVA_OPTS=-Xms512m -Xmx512m`).
  * **인스턴스 확장과 최적화**: 필사적인 리소스 다이어트에도 불구하고 신규 Kafka 브로커 등 다양한 인프라 컨테이너가 동시에 가동되면서 물리적 2GB 메모리로는 가동 불가능 상태에 직면하여 결국 인스턴스 사양을 확장함. 최종 배포 환경에서는 안전하게 **8GB RAM 인스턴스**를 사용했으나, 추가적인 세부 튜닝 시 **4GB RAM** 사양으로도 충분히 안정적인 실서비스 기동이 가능했을 것으로 실측됨.
  * **안전장치 수립**: 우분투 OS 레벨에서 2~4GB의 **스왑 메모리(Swap Memory)** 설정을 필수 반영하도록 설계 가이드 구축.

### 8. Apache Kafka 도입을 통한 결제 완료 비동기 분리 및 장애 격리
* **문제 상황**: 사용자가 결제를 완료하면 주문 생성, 공구 참여 상태 확정, 알림(SSE) 발송 등의 여러 도메인 서비스가 하나의 동기식 트랜잭션으로 강결합되어, 외부 연동 지연이나 예외 발생 시 전체 결제가 실패하거나 응답 시간이 현격히 늘어나는 위험이 존재함.
* **해결 방법**:
  * **이벤트 기반 디커플링**: 결제 완료 및 실패 사실을 로컬 Spring 이벤트(`OrderPaidEvent` / `OrderPaymentFailedEvent`)로 발행하고, `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`를 활용해 데이터베이스 커밋 성공 시점에만 안전하게 Kafka로 이벤트를 발행 (`payment-success-topic`, `payment-failed-topic`).
  * **비동기 컨슈머 연동**: Kafka 컨슈머(`PaymentKafkaConsumer`)가 이 메시지들을 비동기 구독하여 공구 참여 승인 및 주문 확정 로직을 별도 트랜잭션으로 안전하게 비동기 처리하여 결제 메인 비즈니스의 지연을 제거하고 장애 전파를 완벽히 격리함.

---

## 📈 성능 측정 및 인덱스 튜닝 (RDBMS)
* **목표**: 카테고리 필터링이 적용된 상태에서 정렬 조건 변경 시 쿼리 속도 개선
* **조치 및 복합 인덱스 적용**:
  * 테스트 코드(`ProductListServiceIntegrationTest.java`) 구축을 통한 응답 속도 실측.
  * 정렬 컬럼(가격, 판매수, 리뷰수 등)에 단일 인덱스 적용 후, 최종 카테고리 기반 쿼리 처리를 위해 카테고리 필터와 정렬 필드가 결합된 **복합 인덱스(Composite Index)** 적용.
  ```sql
  CREATE INDEX idx_prod_cat_price ON product(category_seq, price);
  ```
* **결과**: 카테고리(ID: 1) 필터링 + 가격 낮은순(`price_asc`) 정렬 50회 테스트 평균 소요 시간 **70.20ms** 확보 (Elasticsearch 도입 후에는 캐싱 필터를 통해 추가로 **5~10ms**로 극대화).

---

## 🛠️ 공동구매 생애주기 (State Machine) 및 선착순 대기열
* **명시적 상태 관리**: `SCHEDULED(대기)` ➡️ `ONGOING(진행)` ➡️ `CONFIRMED(성사)` / `FAILED(무산)` / `STOPPED(강제 중지)`의 라이프사이클을 보장하는 상태 머신 설계.
* **FIFO 선착순 대기열 자동 승격**: 
  * 공동구매 확정 인원 중 이탈자 발생 시 동일 옵션 대기열 최우선 순위가 자동으로 참여자로 승격.
  * 승격자에게 24시간의 결제 유효기간을 부여하고, 공동구매 최종 마감 시 미결제 점유는 자동으로 반납 처리(`FAILED`)되어 대기 슬롯 유효성 보장.
  * 일련의 '이탈-승격-결제 만료' 과정을 단일 트랜잭션 단위로 묶어 신뢰성 높은 비즈니스 연쇄 처리(SAGA 패턴 지향) 구현.
