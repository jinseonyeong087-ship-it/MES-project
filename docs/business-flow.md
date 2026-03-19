# business-flow.md

## 1) 전체 업무 흐름

`생산계획 → 작업지시 → 생산 → 재고 반영 → 로그 기록`

이 프로젝트는 생산 실행 단계에서 발생하는 데이터를 실시간에 가깝게 반영하는 흐름을 목표로 합니다.

---

## 2) 단계별 흐름

### 1) 생산계획 수립
- 제품별 목표 생산량과 일정 확정
- 계획 정보 기반으로 작업지시 생성 준비

### 2) 작업지시 생성
- 작업지시 번호/제품/공정/계획수량 등록
- 상태: `PLANNED`

### 3) 생산 실적 입력
- 작업자가 양품/불량 수량 입력
- 첫 실적 입력 시 상태: `PLANNED → IN_PROGRESS`

### 4) 재고 반영
- 양품 수량 기준으로 재고 증가
- 동시성 고려하여 재고 레코드 잠금 후 반영

### 5) 로그 기록
- 재고 변경 전/후 수량 기록
- 추적 가능하도록 기준 참조(`ref_type`, `ref_id`) 저장

### 6) 작업 완료 처리
- 누적 생산량이 계획수량 이상이면 상태: `IN_PROGRESS → COMPLETED`

---

## 3) 상태 변화 정의

- `PLANNED`: 작업지시 생성 완료, 아직 생산 시작 전
- `IN_PROGRESS`: 생산 실적이 1건 이상 등록됨
- `COMPLETED`: 누적 생산량이 계획수량 도달/초과

### 상태 전이 규칙
- `PLANNED -> IN_PROGRESS` : 첫 생산실적 등록 시
- `IN_PROGRESS -> COMPLETED` : `produced_qty >= planned_qty`
- `COMPLETED` 이후 추가 실적 등록은 기본적으로 제한(운영 정책에 따라 예외 처리 가능)

---

## 4) 재고 반영 로직

```java
@Transactional
public ProductionResultResponse registerResult(RegisterResultRequest req) {
    WorkOrder wo = workOrderRepository.findByIdForUpdate(req.workOrderId());
    validateStatus(wo.getStatus());

    ProductionResult pr = productionResultRepository.save(toEntity(req));

    int produced = req.goodQty() + req.defectQty();
    wo.addProducedQty(produced);
    wo.updateStatusByProgress();

    Inventory inv = inventoryRepository.findByProductIdForUpdate(wo.getProductId());
    int before = inv.getQtyOnHand();
    inv.increase(req.goodQty());
    int after = inv.getQtyOnHand();

    inventoryLogRepository.save(
        InventoryLog.productionIn(inv.getId(), pr.getId(), before, req.goodQty(), after)
    );

    return toResponse(pr, wo, before, after);
}
```

---

## 5) 흐름도

```text
[생산계획]
    |
    v
[작업지시 생성: PLANNED]
    |
    v
[생산 실적 입력]
    |
    +--> [work_order 상태 변경: IN_PROGRESS/COMPLETED]
    |
    +--> [inventory 수량 증가]
    |
    +--> [inventory_log 기록]
    |
    v
[처리 결과 응답]
```
