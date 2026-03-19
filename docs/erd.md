# erd.md

## 1) ERD 개요

Mini MES의 핵심은 **작업지시-생산실적-재고**를 일관된 트랜잭션으로 연결하는 데이터 모델입니다.

```text
product (1) ---- (N) work_order (1) ---- (N) production_result
   |                                 
   |                                  \ 
   |                                   \ 
   +---- (1) ---- (1) inventory         \---- (1) process
                     |
                     +---- (N) inventory_log
```

---

## 2) 테이블 상세

## 2.1 work_order
### 역할
생산 작업지시의 기준 정보 관리 (무엇을, 얼마나, 언제 생산할지)

### 주요 컬럼
- `id` (PK, bigint)
- `work_order_no` (UK, varchar): 작업지시 번호
- `product_id` (FK → product.id)
- `process_id` (FK → process.id)
- `planned_qty` (int)
- `produced_qty` (int): 누적 생산량(`good_qty + defect_qty` 합계)
- `status` (varchar): `PLANNED`, `IN_PROGRESS`, `COMPLETED`
- `planned_date` (date)
- `created_at`, `updated_at` (datetime2)

### 관계
- product : work_order = 1 : N
- process : work_order = 1 : N
- work_order : production_result = 1 : N

---

## 2.2 production_result
### 역할
작업지시에 대한 생산 실적(양품/불량/작업시간) 기록

### 주요 컬럼
- `id` (PK, bigint)
- `work_order_id` (FK → work_order.id)
- `good_qty` (int)
- `defect_qty` (int)
- `total_qty` (int)
- `result_at` (datetime2)
- `operator` (varchar)
- `memo` (varchar)
- `created_at` (datetime2)

### 관계
- work_order : production_result = 1 : N

---

## 2.3 inventory
### 역할
제품별 현재고 관리

### 주요 컬럼
- `id` (PK, bigint)
- `product_id` (FK → product.id, UK)
- `qty_on_hand` (int)
- `safety_stock` (int)
- `updated_at` (datetime2)

### 관계
- product : inventory = 1 : 1

---

## 2.4 inventory_log
### 역할
재고 변경 이력 추적 (감사/분석/문제추적)

### 주요 컬럼
- `id` (PK, bigint)
- `inventory_id` (FK → inventory.id)
- `change_type` (varchar): `PRODUCTION_IN`, `ADJUSTMENT`, `MANUAL_CORRECTION`
- `ref_type` (varchar): `WORK_ORDER`, `PRODUCTION_RESULT`
- `ref_id` (bigint)
- `before_qty` (int)
- `change_qty` (int)
- `after_qty` (int)
- `changed_at` (datetime2)
- `changed_by` (varchar)

### 관계
- inventory : inventory_log = 1 : N

### 참조 기준
- 생산 실적 입력으로 발생한 재고 증가 로그는 `ref_type='PRODUCTION_RESULT'`, `ref_id=production_result.id`로 저장
- 필요 시 집계/추적 화면에서 `production_result -> work_order`를 따라 작업지시 기준으로 역추적

---

## 2.5 product
### 역할
생산 대상 제품 마스터

### 주요 컬럼
- `id` (PK, bigint)
- `product_code` (UK, varchar)
- `product_name` (varchar)
- `unit` (varchar)
- `use_yn` (char)
- `created_at`, `updated_at` (datetime2)

### 관계
- product : work_order = 1 : N
- product : inventory = 1 : 1

---

## 2.6 process
### 역할
공정 마스터 (예: 조립, 검사, 포장)

### 주요 컬럼
- `id` (PK, bigint)
- `process_code` (UK, varchar)
- `process_name` (varchar)
- `description` (varchar)
- `use_yn` (char)

### 관계
- process : work_order = 1 : N

---

## 3) 인덱스 설계

실행 빈도 높은 조회/조인을 기준으로 인덱스를 설계합니다.

- `work_order`
  - `UK(work_order_no)`
  - `IDX_work_order_status_planned_date(status, planned_date)`
  - `IDX_work_order_product_id(product_id)`

- `production_result`
  - `IDX_production_result_work_order_id_result_at(work_order_id, result_at DESC)`

- `inventory`
  - `UK(product_id)`

- `inventory_log`
  - `IDX_inventory_log_inventory_id_changed_at(inventory_id, changed_at DESC)`
  - `IDX_inventory_log_ref_type_ref_id(ref_type, ref_id)`

---

## 4) 트랜잭션 고려 포인트

생산 실적 입력은 다음 순서를 **하나의 트랜잭션**으로 처리합니다.

1. `work_order` 행 조회 + 상태 검증
2. `production_result` insert
3. `work_order.produced_qty` 누적, 상태 갱신
4. `inventory.qty_on_hand` 증가 (동시성 제어)
5. `inventory_log` insert
6. commit

### 동시성 제어
- MS-SQL 기준으로 재고/작업지시는 락 힌트(`UPDLOCK`, `ROWLOCK` 등) 또는 비관적 락으로 동시 업데이트 충돌 방지
- 낙관적 락(버전 컬럼) 또는 비관적 락 중 트래픽 패턴에 맞게 선택

---

## 5) SQLD 기출 핵심 포인트 (인덱스 / 정규화 / 트랜잭션)

### 5.1 인덱스
- 조회 조건과 조인 키에 인덱스 부여
- 카디널리티/선택도를 고려한 복합 인덱스 설계
- 과도한 인덱스는 쓰기 성능 저하 유발 → 운영 쿼리 기준으로 최소화

### 5.2 정규화
- 마스터(`product`, `process`)와 트랜잭션(`work_order`, `production_result`, `inventory_log`) 분리
- 중복 데이터 최소화로 이상(삽입/수정/삭제 이상) 방지
- 조회 성능 이슈 시 필요한 범위에서만 비정규화 검토

### 5.3 트랜잭션
- 원자성: 생산실적/재고/로그는 한 단위로 commit/rollback
- 일관성: 상태 전이 규칙(PLANNED → IN_PROGRESS → COMPLETED) 강제
- 격리성: 동시 입력 시 락/격리수준으로 재고 불일치 방지
- 지속성: commit 이후 로그 기반 이력 추적 가능
