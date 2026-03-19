# Mini MES

작업지시(Work Order) 기반으로 생산 실적을 입력하면 재고가 자동 반영되는 **Mini MES (Manufacturing Execution System)** 프로젝트입니다.  
핵심은 **DB 중심 설계**, **트랜잭션 기반 재고 처리**, **상태 관리(PLANNED → IN_PROGRESS → COMPLETED)** 입니다.

---

## 1) 프로젝트 소개

MES(Manufacturing Execution System)는 생산 현장의 실행 정보를 관리하는 시스템입니다.  
이 프로젝트는 생산 계획 이후 실제 현장에서 일어나는 아래 과정을 백엔드 중심으로 구현합니다.

- 작업지시 생성/조회
- 생산 실적 입력
- 생산 완료 시 재고 자동 반영
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

## 4) 실행 방법 (Docker 기준)

## 사전 준비
- Docker / Docker Compose 설치
- Git clone

```bash
git clone <your-repo-url>.git
cd mes-project
```

## 실행
```bash
docker-compose up -d --build
```

## 상태 확인
```bash
docker-compose ps
```

## 로그 확인
```bash
docker-compose logs -f backend
```

## 종료
```bash
docker-compose down
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
    └── troubleshooting.md
```

---

## 6) 한 줄 요약 (면접용)

**“생산 실적 입력을 트랜잭션으로 처리해 작업지시 상태와 재고를 동시에 갱신하고, 로그까지 남겨 정합성과 추적성을 확보한 Mini MES 백엔드 프로젝트입니다.”**
