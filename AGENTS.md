# AGENTS.md - Project Constitution & Architecture Guidelines

위버스컴퍼니 수준의 대규모 트래픽(초당 1,000건 이상)을 처리하는 **한정판 굿즈 선착순 예매 시스템**을 위한 아키텍처 원칙 및 협업 헌법입니다.
모든 서브 에이전트(@Engineer, @SRE, @TechWriter, @PerformanceArchitect 등)와 메인 오케스트레이터(@Mentor)는 아래 규정을 엄수해야 합니다.

---

## 1. Architecture: Modular Monolith, EDA & CQRS
- **Modular Monolith 패키지 격리**: 도메인별(`user`, `product`, `order`)로 책임을 엄격히 분리하여 모듈 간 불필요한 결합을 차단한다.
- **EDA (Event-Driven Architecture) 기반 비동기 주문 처리**: 
  - 선착순 구매/주문(Write) 요청은 DB에 직접 동기식으로 INSERT하지 않는다.
  - 유효성 검증 후 Kafka 이벤트 메시지를 발행하고, 백그라운드 Consumer가 순차적으로 처리하는 비동기 파이프라인 구조를 채택한다.
- **CQRS (Command Query Responsibility Segregation)**:
  - 대규모 선착순 트래픽에서 병목이 되는 '상품 상세 및 잔여 재고 조회(Read)'는 RDBMS가 아닌 Redis 캐시 기반으로 우선 조회한다.
  - 쓰기(Command: 주문/결제/재고차감)와 읽기(Query: 상품/재고 조회)의 관심사와 저장소를 논리적으로 분리한다.
- **API-First & 단방향 의존성**: 명확한 DTO 스펙을 우선 정의하고, `Controller -> Service -> Repository` 단방향 의존성을 엄수한다.

---

## 2. Clean Code & Framework Standard
- **Entity 직접 반환 절대 금지**: JPA Entity를 Controller/API 응답으로 직접 노출하는 것은 보안 및 결합도 측면에서 엄격히 금지하며, 반드시 전용 Request/Response DTO로 변환하여 처리한다.
- **생성자 기반 의존성 주입**: 필드 주입(`@Autowired`)을 금지하고, Lombok의 `@RequiredArgsConstructor`를 통한 final 필드 생성자 주입만을 허용한다.
- **가독성 및 응집도**: 각 컴포넌트는 단일 책임 원칙(SRP)을 준수하며, 명확한 네이밍 컨벤션을 유지한다.

---

## 3. Data Integrity & Persistence Standard
- **지연 로딩 기본 원칙**: 모든 N:1 (`@ManyToOne`), 1:1 (`@OneToOne`) 엔티티 연관관계는 `fetch = FetchType.LAZY` (지연 로딩)를 기본으로 적용한다.
- **트랜잭션 격리 및 범위**: 비즈니스 트랜잭션은 서비스 계층에서 `@Transactional`로 명확히 관리하며, 읽기 전용 작업은 `@Transactional(readOnly = true)`를 적용한다.

---

## 4. Core Logic & Concurrency Control (학습 주도권 방어 영역)
- **동시성 제어 핵심 로직 선제 작성 금지**: 대규모 트래픽 하에서의 분산 락(Distributed Lock via Redis/Redisson), 비동기 메시지 큐(Kafka 토픽 발행/소비), 비관적/낙관적 락 등 동시성 제어 핵심 로직은 **AI가 임의로 코드를 완성하지 않는다**.
- **TODO 태그 주입 필수**: 해당 핵심 영역은 학습자가 아키텍처 설계와 논리를 직접 고민하고 도출할 수 있도록 다음 형태의 주석만 작성해 둔다:
  ```java
  // TODO: [학습자 주도 영역] 논리 적용 필요 - 분산 락 / 비동기 큐 동시성 제어
  ```
- **학습자 승인**: 핵심 비즈니스 로직 및 동시성 제어 메커니즘은 반드시 학습자가 논리를 제시하고 승인한 후에만 실체화한다.


## 5. Git push & commit
- **자동 커밋**: 코드가 수정될때 마다 자동으로 커밋하고 push 한다. 커밋 메시지는 한글로 작성한다.
- **테스트 자동화**: 컴파일 에러가 발생하면 자동으로 수정하고 push 한다. 테스트가 통과하면 자동으로 커밋하고 push 한다.
- **메인 브랜치**: main, develop, release, feature/이슈번호 형태로 브랜치를 관리한다.
- **이슈 번호**: 모든 feature 브랜치에는 이슈 번호를 포함한다.
- **PR 리뷰**: 모든 feature 브랜치는 main 브랜치로 merge 하기 전에 PR을 생성하고 리뷰를 받는다. 리뷰어는 2명 이상이어야 한다.
- **PR 제목**: [FEAT/FIX/DOCS] 이슈번호: 간단한 제목 형태로 작성한다.

---

## 6. Mentoring & Critical Review (이성적·냉철한 비판 원칙)
- **무조건적 긍정 금지**: 학습자의 설계 제안이나 질문에 대해 단순한 칭찬이나 무조건적인 동조를 절대 금지한다.
- **냉철한 트레이드오프 분석**: 모든 아키텍처 제안에 대해 잠재적 장애 위험(Race Condition, 분산 트랜잭션 실패, 메모리/커넥션 고갈, 타임아웃 엣지 케이스)과 복잡도 비용(Operational Cost)을 비판적으로 파헤쳐 검증한다.
- **엔지니어링 근거 기반 피드백**: "왜 그 방식이 특정 상황에서 실패할 수 있는지", "어떤 병목과 부작용이 따르는지"를 객관적 수치와 실무 사례 기반으로 냉정하게 짚고 대안을 도출하게 한다.

