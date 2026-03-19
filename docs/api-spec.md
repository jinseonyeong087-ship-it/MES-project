# api-spec.md

> Base URL: `/api/v1`

---

## 1) 작업지시 생성 API

### POST /work-orders

### Request
```json
{
  "workOrderNo": "WO-20260319-001",
  "productId": 1001,
  "processId": 2001,
  "plannedQty": 1000,
  "plannedDate": "2026-03-20"
}
```

### Response (201 Created)
```json
{
  "id": 1,
  "workOrderNo": "WO-20260319-001",
  "productId": 1001,
  "processId": 2001,
  "plannedQty": 1000,
  "producedQty": 0,
  "status": "PLANNED",
  "plannedDate": "2026-03-20",
  "createdAt": "2026-03-19T20:00:00"
}
```

### 설명
- 신규 작업지시를 생성합니다.
- 생성 시 기본 상태는 `PLANNED` 입니다.
- `workOrderNo`는 유니크해야 합니다.

---

## 2) 생산 실적 입력 API

### POST /production-results

### Request
```json
{
  "workOrderId": 1,
  "goodQty": 300,
  "defectQty": 5,
  "resultAt": "2026-03-20T09:30:00",
  "operator": "kim",
  "memo": "1차 생산 실적"
}
```

### Response (201 Created)
```json
{
  "productionResultId": 10,
  "workOrderId": 1,
  "workOrderStatus": "IN_PROGRESS",
  "accumulatedProducedQty": 305,
  "inventory": {
    "productId": 1001,
    "beforeQty": 1200,
    "changeQty": 300,
    "afterQty": 1500
  },
  "logged": true,
  "createdAt": "2026-03-20T09:30:01"
}
```

### 설명
- 생산 실적을 등록합니다.
- 내부적으로 다음 작업을 **단일 트랜잭션**으로 처리합니다.
  1. `production_result` 저장
  2. `work_order.produced_qty`/`status` 갱신
  3. `inventory.qty_on_hand` 증가(양품 기준)
  4. `inventory_log` 기록

---

## 3) 재고 조회 API

### GET /inventories/{productId}

### Response (200 OK)
```json
{
  "productId": 1001,
  "productCode": "P-1001",
  "productName": "완제품A",
  "qtyOnHand": 1500,
  "safetyStock": 300,
  "updatedAt": "2026-03-20T09:30:01"
}
```

### 설명
- 제품 기준 현재고를 조회합니다.
- 운영 화면에서 재고 모니터링에 사용합니다.

---

## 4) 공통 에러 응답

```json
{
  "timestamp": "2026-03-20T10:11:12",
  "status": 409,
  "error": "CONFLICT",
  "code": "WORK_ORDER_STATUS_INVALID",
  "message": "완료된 작업지시는 생산 실적을 추가할 수 없습니다.",
  "path": "/api/v1/production-results"
}
```

### 주요 에러 케이스
- `400 BAD_REQUEST`: 필수값 누락, 음수 수량
- `404 NOT_FOUND`: 작업지시/제품 없음
- `409 CONFLICT`: 상태 충돌, 중복 작업지시 번호
- `500 INTERNAL_SERVER_ERROR`: 트랜잭션 실패
