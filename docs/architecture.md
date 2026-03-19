# architecture.md

## 1) 전체 시스템 구조

Mini MES는 Spring Boot + MS-SQL 기반의 **단일 애플리케이션(모놀리식) 구조**입니다.  
API 서버 하나가 작업지시, 생산 실적, 재고, 로그 도메인을 함께 처리하고, 모든 핵심 데이터는 MS-SQL에 저장됩니다.

- Client: 웹/모바일/관리자 화면
- Server: Spring Boot REST API
- DB: MS-SQL
- Runtime: Docker Compose

---

## 2) Spring Boot + MS-SQL 단일 구조

### 구성 원칙
- 도메인별 패키지 분리(WorkOrder, Production, Inventory)
- 서비스 계층에서 트랜잭션 제어
- Repository(JPA/MyBatis 등)로 DB 접근
- 상태/재고 반영은 반드시 서비스 계층에서 단일 트랜잭션 처리

### 장점
- 빠른 개발 및 운영 단순성
- 트랜잭션 경계 관리가 쉬움
- 데이터 정합성 요구가 높은 MES 초기 버전에 적합

---

## 3) Docker 실행 구조

```text
+--------------------------+        +----------------------+
|      backend container   |        |   mssql container    |
|  - Spring Boot App       |<------>|  - Mini MES Schema   |
|  - REST API              |  JDBC  |  - Tables/Indexes    |
+--------------------------+        +----------------------+
          ^
          |
       HTTP/JSON
          |
+--------------------------+
|       User / Client      |
+--------------------------+
```

---

## 4) 요청 흐름 (User → API → DB)

1. User가 작업지시 생성 또는 생산 실적 입력 요청
2. Controller가 요청 검증 후 Service 호출
3. Service에서 비즈니스 규칙 검사 (상태, 수량, 중복 등)
4. Repository를 통해 MS-SQL 읽기/쓰기
5. 트랜잭션 커밋 시 작업지시/재고/로그 동시 반영
6. API 응답 반환

---

## 5) 데이터 흐름

### 생산 실적 입력 시 핵심 데이터 흐름

```text
[production_result insert]
        |
        v
[work_order 상태 변경]
 PLANNED -> IN_PROGRESS -> COMPLETED
        |
        v
[inventory 수량 증가]
        |
        v
[inventory_log 기록]
(change_type=PRODUCTION_IN, before_qty, change_qty, after_qty)
```

### 트랜잭션 경계
- `production_result` 저장
- `work_order` 상태 업데이트
- `inventory` 수량 업데이트
- `inventory_log` 저장

위 4개는 **하나의 트랜잭션**으로 처리합니다. 하나라도 실패하면 전체 롤백합니다.
