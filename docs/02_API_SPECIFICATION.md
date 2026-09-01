# 02. 대규모 선착순 구매 시스템 RESTful API 명세서

> **원칙 준수 (AGENTS.md):**
> 1. API-First Design 원칙을 엄수하며 모든 Request/Response는 전용 DTO 스펙을 정의한다.
> 2. JPA Entity를 API 응답으로 절대 노출하지 않는다.
> 3. HTTP Status Code 및 REST 규약을 준수한다.

---

## 1. 대기열 API (Queue Domain)

### 1.1. 대기열 진입 및 토큰 발급
- **Endpoint:** `POST /api/v1/queue/enter`
- **Description:** 특정 상품 선착순 구매를 위한 대기열에 진입하고 고유 대기열 토큰을 발급받습니다.
- **Request Headers:**
  - `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "userId": 1001,
    "productId": 1
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "token": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "status": "WAITING",
    "rank": 352,
    "estimatedWaitSeconds": 7,
    "issuedAt": "2026-08-31T21:40:00"
  }
  ```

---

### 1.2. 내 대기 순번 및 상태 조회 (Polling)
- **Endpoint:** `GET /api/v1/queue/status`
- **Description:** 발급받은 대기열 토큰의 현재 순번과 입장 허가(`ACTIVE`) 여부를 확인합니다.
- **Request Headers:**
  - `X-Queue-Token: d290f1ee-6c54-4b01-90e6-d701748f0851`
- **Query Parameters:**
  - `productId=1`
- **Response Case 1 (대기 중 - 200 OK):**
  ```json
  {
    "token": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "status": "WAITING",
    "rank": 14,
    "estimatedWaitSeconds": 1,
    "message": "현재 대기 중입니다. 잠시만 기다려주세요."
  }
  ```
- **Response Case 2 (입장 허용 - 200 OK):**
  ```json
  {
    "token": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "status": "ACTIVE",
    "rank": 0,
    "estimatedWaitSeconds": 0,
    "message": "입장이 허용되었습니다. 5분 내에 주문을 완료해주세요.",
    "expiresAt": "2026-08-31T21:45:00"
  }
  ```
- **Response Case 3 (토큰 만료/유효하지 않음 - 401 Unauthorized):**
  ```json
  {
    "code": "INVALID_QUEUE_TOKEN",
    "message": "유효하지 않거나 만료된 대기열 토큰입니다. 다시 대기열에 진입해주세요."
  }
  ```

---

## 2. 상품 조회 API (Product Domain - CQRS Read)

### 2.1. 상품 상세 정보 조회
- **Endpoint:** `GET /api/v1/products/{productId}`
- **Description:** 상품 기본 정보 및 상태를 조회합니다. (Redis Cache 우선 조회)
- **Response (200 OK):**
  ```json
  {
    "id": 1,
    "name": "BTS 2026 WORLD TOUR 한정판 스페셜 패키지",
    "description": "전 세계 100개 한정 공식 굿즈 패키지",
    "price": 189000.00,
    "availableStock": 100,
    "status": "ON_SALE",
    "salesStartAt": "2026-08-31T21:00:00",
    "salesEndAt": "2026-08-31T23:59:59"
  }
  ```

---

### 2.2. 실시간 잔여 재고 조회
- **Endpoint:** `GET /api/v1/products/{productId}/stock`
- **Description:** 초단위로 변경되는 실시간 잔여 재고 수량만 가볍게 조회합니다.
- **Response (200 OK):**
  ```json
  {
    "productId": 1,
    "availableStock": 42
  }
  ```

---

## 3. 선착순 주문 API (Order Domain - EDA Write)

### 3.1. 선착순 주문 요청 접수 (비동기 처리)
- **Endpoint:** `POST /api/v1/orders`
- **Description:** 대기열을 통과한 활성 토큰을 제시하여 선착순 주문을 접수합니다. 비동기로 큐에 적재되며 즉시 접수 번호를 응답받습니다.
- **Request Headers:**
  - `Content-Type: application/json`
  - `X-Queue-Token: d290f1ee-6c54-4b01-90e6-d701748f0851` (필수)
- **Request Body:**
  ```json
  {
    "userId": 1001,
    "productId": 1,
    "deliveryAddressId": 10,
    "quantity": 1
  }
  ```
- **Response (202 Accepted):**
  ```json
  {
    "orderNumber": "ORD-20260831-8F3A29B1",
    "status": "ACCEPTED",
    "message": "주문 요청이 성공적으로 접수되어 백그라운드에서 처리 중입니다."
  }
  ```
- **Error Cases:**
  - `400 Bad Request`: 수량 미달/초과, 잘못된 파라미터
  - `401 Unauthorized`: 유효하지 않거나 ACTIVE 상태가 아닌 대기열 토큰
  - `409 Conflict`: 품절(SOLD_OUT) 또는 이미 1회 구매 완료된 유저

---

### 3.2. 주문 최종 처리 상태 단건 조회 (Polling/확인용)
- **Endpoint:** `GET /api/v1/orders/{orderNumber}`
- **Description:** 비동기로 접수된 주문의 최종 데이터베이스 영속화 상태를 확인합니다.
- **Response (200 OK):**
  ```json
  {
    "orderNumber": "ORD-20260831-8F3A29B1",
    "userId": 1001,
    "productId": 1,
    "productName": "BTS 2026 WORLD TOUR 한정판 스페셜 패키지",
    "quantity": 1,
    "totalAmount": 189000.00,
    "status": "PAID",
    "createdAt": "2026-08-31T21:42:15"
  }
  ```
- **Status Enum:**
  - `PENDING`: 비동기 큐에서 처리 대기 중
  - `PAID`: 주문 및 결제 정상 체결 완료
  - `FAILED`: 재고 부족 또는 처리 실패로 인한 주문 취소

---

### 3.3. 유저별 주문 내역 목록 조회
- **Endpoint:** `GET /api/v1/orders/users/{userId}`
- **Description:** 특정 사용자가 주문한 전체 주문 목록을 조회합니다.
- **Response (200 OK):**
  ```json
  [
    {
      "orderNumber": "ORD-20260831-8F3A29B1",
      "productId": 1,
      "productName": "BTS 2026 WORLD TOUR 한정판 스페셜 패키지",
      "quantity": 1,
      "totalAmount": 189000.00,
      "status": "PAID",
      "createdAt": "2026-08-31T21:42:15"
    }
  ]
  ```
