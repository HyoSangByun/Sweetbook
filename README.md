# SweetBook RunBook 포토북 서비스

## 1. 서비스 소개
- 한 줄 소개: 러닝 활동 데이터(Strava)를 기반으로 SweetBook 포토북을 생성하고 주문까지 연결하는 서비스입니다.
- 타겟 고객: 러닝/운동 기록을 사진과 함께 책으로 남기고 싶은 개인 사용자, 러닝 크루 운영자
- 주요 기능:
  - 활동 데이터(CSV) 업로드 및 월별 활동 선택
  - 단계별 포토북 생성(드래프트 → 사진 업로드 → 표지/내지 적용 → 최종화)
  - 주문 생성/조회, 주문 취소, 배송지 변경
  - 크레딧 조회, 충전

## 2. 실행 방법
아래 순서대로 복사-붙여넣기 해서 실행할 수 있습니다.

### 2-1. 백엔드 실행 (Spring Boot)
```bash
cd server

# 환경변수 설정
cp .env.example .env
# Windows PowerShell: Copy-Item .env.example .env

# .env 파일에 API Key 입력
# SWEETBOOK_SANDBOX_API_KEY=your-sandbox-api-key

# 실행
./gradlew bootRun
# Windows: .\gradlew.bat bootRun
```

### 2-2. 프론트엔드 실행 (Vue)
```bash
cd client

# 설치
npm install

# 실행
npm run dev
```

브라우저 접속:
- 프론트: `http://localhost:5173` (Vite 기본)
- 백엔드: `http://localhost:8080`

콘텐츠 더미 데이터(러닝 데이터, 샘플 사진)는 server/src/main/resources/data 디렉터리에 포함되어 있습니다.

## 3. 사용한 API 목록 (SweetBook Book Print API)
| API | 용도 |
|---|---|
| `GET /v1/credits` | 크레딧 잔액 조회 |
| `POST /v1/credits/sandbox/charge` | 크레딧 충전 |
| `POST /v1/books` | 포토북 초안 생성 |
| `GET /v1/books` | 생성된 책 목록 조회 |
| `GET /v1/books/{bookUid}` | 책 상세 조회(필요 시) |
| `POST /v1/books/{bookUid}/photos` | 책 이미지 업로드 |
| `GET /v1/books/{bookUid}/photos` | 업로드 이미지 목록 조회 |
| `POST /v1/books/{bookUid}/cover` | 표지 적용 |
| `POST /v1/books/{bookUid}/contents` | 내지 페이지 추가 |
| `POST /v1/books/{bookUid}/finalization` | 책 최종화 |
| `GET /v1/templates` | 템플릿 목록 조회 |
| `GET /v1/templates/{templateUid}` | 템플릿 상세/파라미터 메타데이터 조회 |
| `GET /v1/book-specs` | 판형 목록 조회 |
| `POST /v1/orders` | 주문 생성 |
| `GET /v1/orders` | 주문 목록 조회 |
| `GET /v1/orders/{orderUid}` | 주문 상세 조회 |
| `POST /v1/orders/{orderUid}/cancel` | 주문 취소 |
| `PATCH /v1/orders/{orderUid}/shipping` | 배송지 변경 |

## 4. AI 도구 사용 내역
| AI 도구 | 활용 내용 |
|---|---|
| Claude Code | 백엔드 API 라우팅 구조 설계 및 DTO/서비스 리팩토링 아이디어 검토, 작업용 md파일 생성 |
| ChatGPT |  에러 원인 분석, 더미 데이터 생성, 문서화(README) 초안 작성 |
| GEMINI CLI |  프론트엔드 페이지 및 컴포넌트 구현 |
| CODEX |  백엔드 API 및 서비스 로직 구현, 일부 프론트엔드 코드 작성 |
| CodeRabbit |  PR 단위 코드 리뷰 및 품질 개선 포인트 점검 |

## 5. 설계 의도
- 왜 이 서비스를 선택했는지:
  - 러닝의 유행과 더불어 러닝 데이터는 활용 가치가 높고, 결과물을 실물 포토북으로 전환하면 사용자 만족도와 재구매 가능성이 큽니다.
- 비즈니스 가능성:
  - 개인 러닝 기록 보관 수요 + 선물/기념일 수요가 결합된 형태로, 시즌성 이벤트(마라톤 완주, 연말 결산)와 잘 맞습니다.
  - 크루/커뮤니티 단위의 단체 주문 확장 가능성이 높습니다.
- 더 시간이 있었다면 추가할 기능:
  - 템플릿 실시간 미리보기(클라이언트 렌더링 기반)
  - 주문/배송 상태 웹훅 연동 알림
  - 앨범별 주문 생성 전용 화면 완성(무앨범 주문 생성 플로우 정리)
  - 관리자용 통계 대시보드(주문/전환율/재구매율)
  - NIKE RUN CLUB 데이터 동기화

