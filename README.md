# Mini MES

작업지시(Work Order) 기반으로 생산 실적을 입력하면 재고가 자동 반영되는 **Mini MES (Manufacturing Execution System)** 프로젝트입니다.  
핵심은 **DB 중심 설계**, **트랜잭션 기반 재고 처리**, **상태 관리(PLANNED → IN_PROGRESS → COMPLETED)** 입니다.

---

## 1) 프로젝트 소개

MES(Manufacturing Execution System)는 생산 현장의 실행 정보를 관리하는 시스템입니다.  
이 프로젝트는 생산 계획 이후 실제 현장에서 일어나는 아래 과정을 백엔드 중심으로 구현합니다.

- 작업지시 생성
- 생산 실적 입력
- 생산 실적 입력 시 양품 기준 재고 자동 반영
- 재고 변경 이력 로그 추적

> 목표: “생산 데이터 입력”과 “재고 반영”을 분리하지 않고, 하나의 트랜잭션으로 처리해 데이터 정합성을 보장한다.

---

## 2) 주요 기능

### 작업지시(Work Order)
- 작업지시 생성
- 제품/공정/계획수량/계획일자 관리
- 작업지시 상태 관리 (`PLANNED`, `IN_PROGRESS`, `COMPLETED`)

### 생산(Production Result)
- 생산 실적 입력 (양품/불량/작업시간 등)
- 입력 시 작업지시 상태 자동 변경
- 완료 조건 충족 시 상태 `COMPLETED` 전환

### 재고(Inventory)
- 생산 실적 등록 시 완제품 재고 증가
- 재고 변경 내역 `inventory_log`에 저장
- 재고 조회 API 제공

---

## 3) 기술 스택

- **Backend**: Spring Boot (Java)
- **Database**: MS-SQL
- **Infra/Deploy**: Docker, Docker Compose
- **CI/CD**: GitHub Actions
- **API 문서화**: Swagger/OpenAPI 스타일 명세

---

## 4) 실행 방법

### 4.1 사전 준비
- Docker / Docker Compose v2 설치
- Java 17+
- Maven (또는 IDE 내장 Maven)

```bash
git clone <your-repo-url>.git
cd mes-project
```

### 4.2 MS-SQL 실행 (Docker)
```bash
docker compose up -d
docker compose ps
```

### 4.3 DB 생성 및 스키마/시드 적용
```bash
# DB 생성
docker exec -it mes-mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'MesProject!2026' -C -Q "IF DB_ID('mini_mes') IS NULL CREATE DATABASE mini_mes;"

# 스키마 적용
docker exec -i mes-mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'MesProject!2026' -C -I -d mini_mes -b < ./sql/01_schema.sql

# 시드 적용(선택)
docker exec -i mes-mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P 'MesProject!2026' -C -I -d mini_mes -b < ./sql/02_seed.sql
```

### 4.4 백엔드 실행
```bash
cd backend
mvn spring-boot:run
```

헬스체크:
```bash
curl http://localhost:8080/api/v1/health
```

### 4.5 프론트 실행
```bash
cd frontend
python3 -m http.server 5500
```
브라우저: `http://localhost:5500`

### 4.6 종료
```bash
docker compose down
```

---

## 5) 프로젝트 구조

```text
mes-project/
├── backend/
├── docker-compose.yml
├── README.md
└── docs/
    ├── architecture.md
    ├── erd.md
    ├── api-spec.md
    ├── business-flow.md
    ├── troubleshooting.md
    └── db-environment.md
```

---

## 6) 현재 구현 API (기준)

### Write API
- `POST /api/v1/work-orders`
- `POST /api/v1/production-results`

### Read API
- `GET /api/v1/health`
- `GET /api/v1/work-orders`
- `GET /api/v1/production-results`
- `GET /api/v1/inventories/{productId}`
- `GET /api/v1/products`
- `GET /api/v1/processes`

> 참고: `inventories/{productId}`의 `productId`는 `product_code(P-1001)`가 아니라 DB의 `product.id`입니다.

---

## 7) DB 환경 구성

DB 환경 구성 상세는 아래 문서에 정리했습니다.

- [docs/db-environment.md](./docs/db-environment.md)

---

## 8) 증적 문서

- [WORK_EVIDENCE_2026-03-19_2026-03-20.md](./WORK_EVIDENCE_2026-03-19_2026-03-20.md)

