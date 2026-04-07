# 주문 API 계약서 (연동 고정)

이 문서는 현재 백엔드 구현 기준의 주문 API 계약을 고정하기 위한 문서입니다.

## 공통 규칙

- Base Path: `/api`
- 인증: `Authorization: Bearer {accessToken}` 필수
- 실패 시 공통 에러 형식(`ApiResponse.error`) 사용

## 주문 생성

- `POST /api/albums/{albumId}/orders`
- 요청 필드: `items`, `shipping`, `externalRef`, `externalUserId`
- 서버 동작
  - `items[*].bookUid`는 클라이언트 입력값을 그대로 신뢰하지 않고, 서버가 보유한 `album.bookUid`로 강제합니다.
  - idempotency key는 `order-{albumId}-{externalRef}` 형식을 사용합니다.
- 응답 필드
  - `orderId`, `orderUid`, `externalRef`, `status`, `lastErrorMessage`
  - `remoteOrderStatusCode`, `remoteOrderStatusDisplay`, `remoteOrderedAt`, `createdAt`

## 주문 취소

- `POST /api/albums/{albumId}/orders/{orderId}/cancel`
- 취소 가능 원격 상태코드: `20(PAID)`, `25(PDF_READY)`
- 서버 동작
  - 소유권/상태 검증 후 Sweetbook 취소 API 호출
  - 원격 호출은 DB 트랜잭션 밖에서 실행, 응답 반영만 짧은 트랜잭션으로 처리

## 배송지 변경

- `PATCH /api/albums/{albumId}/orders/{orderId}/shipping`
- 변경 가능 원격 상태코드: `20(PAID)`, `25(PDF_READY)`, `30(CONFIRMED)`
- 서버 동작
  - patch 필드는 1개 이상 필요
  - 원격 호출은 DB 트랜잭션 밖에서 실행, 응답/payload 반영은 짧은 트랜잭션으로 처리

## 주문 조회

- 목록: `GET /api/albums/{albumId}/orders`
- 상세: `GET /api/albums/{albumId}/orders/{orderId}`

## Webhook 수신

- `POST /api/webhooks/sweetbook/orders`
- JWT 없이 접근 허용, 서명 검증 필수
- 필수 헤더
  - `X-Webhook-Signature`
  - `X-Webhook-Timestamp` (epoch seconds)
  - `X-Webhook-Event`
  - `X-Webhook-Delivery`
- 서명 검증
  - 문자열: `{timestamp}.{rawBody}`
  - 알고리즘: `HMAC-SHA256`
- timestamp 허용 오차
  - `app.sweetbook.webhook-timestamp-tolerance`
- 본문 파싱
  - 단일 객체, 배열, `data`, `events` 래핑 형태를 모두 지원

## 원격 상태코드 매핑

- `20(PAID)`, `25(PDF_READY)`, `30(CONFIRMED)`, `40(IN_PRODUCTION)`, `50(PRODUCTION_COMPLETE)`, `60(SHIPPED)` -> `CREATED`
- `70(DELIVERED)` -> `COMPLETED`
- `80(CANCELLED)`, `81(CANCELLED_REFUND)` -> `CANCELLED`
- `90(ERROR)` -> `FAILED`

## 상태 전이 규칙

- 같은 `X-Webhook-Delivery`는 중복 처리하지 않음
- 역전이(낮은 단계 코드로 회귀)는 무시
- 예외: `order.restored`는 `CANCELLED -> CREATED` 복구 허용

## 주요 에러 코드

- `AUTH_001`: 인증 필요 (Webhook 서명/타임스탬프 검증 실패 포함)
- `AUTH_002`: 권한 없음
- `ALBUM_001`: 앨범 없음
- `COMMON_001`: 잘못된 요청
- `SWEETBOOK_001`: Sweetbook API 호출 실패
