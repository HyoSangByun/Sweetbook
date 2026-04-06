# 주문 API 계약서 (연동 고정)

이 문서는 현재 백엔드 구현 기준의 주문 API 계약을 고정하기 위한 문서다.

## 공통 규칙

- Base Path: `/api`
- 인증: `Authorization: Bearer {accessToken}` 필수
- 응답 포맷:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2026-04-06T11:40:00+09:00"
}
```

## 주문 생성

- `POST /api/albums/{albumId}/orders`
- Request JSON:

```json
{
  "items": [
    { "bookUid": "bk_abc123", "quantity": 1 }
  ],
  "shipping": {
    "recipientName": "홍길동",
    "recipientPhone": "010-1234-5678",
    "postalCode": "06101",
    "address1": "서울시 강남구 테헤란로 123",
    "address2": "4층 401호",
    "memo": "부재시 경비실"
  },
  "externalRef": "PARTNER-ORDER-001",
  "externalUserId": "user-001"
}
```

- Response `data`:
  - `orderId`
  - `orderUid`
  - `externalRef`
  - `status` (`REQUESTED`, `CREATED`, `COMPLETED`, `CANCELLED`, `FAILED`)
  - `lastErrorMessage`
  - `remoteOrderStatusCode`
  - `remoteOrderStatusDisplay`
  - `remoteOrderedAt`
  - `createdAt`

## 주문 취소

- `POST /api/albums/{albumId}/orders/{orderId}/cancel`
- Request JSON:

```json
{
  "cancelReason": "고객 요청"
}
```

- 취소 가능 조건:
  - 원격 상태코드 `PAID(20)` 또는 `PDF_READY(25)`
- Response `data`:
  - 주문 상세 조회 응답과 동일

## 배송지 변경

- `PATCH /api/albums/{albumId}/orders/{orderId}/shipping`
- Request JSON (변경할 필드만 전달):

```json
{
  "recipientName": "김영희",
  "address1": "서울시 서초구 반포대로 100"
}
```

- 변경 가능 조건:
  - 원격 상태코드 `PAID(20)`, `PDF_READY(25)`, `CONFIRMED(30)`
- Response `data`:
  - 주문 상세 조회 응답과 동일

## 주문 목록 조회

- `GET /api/albums/{albumId}/orders`
- Response `data`: 배열
  - `orderId`
  - `orderUid`
  - `externalRef`
  - `status`
  - `lastErrorMessage`
  - `remoteOrderStatusCode`
  - `remoteOrderStatusDisplay`
  - `remoteOrderedAt`
  - `createdAt`
  - `updatedAt`

## 주문 상세 조회

- `GET /api/albums/{albumId}/orders/{orderId}`
- Response `data`:
  - `orderId`
  - `orderUid`
  - `externalRef`
  - `status`
  - `lastErrorMessage`
  - `remoteOrderStatusCode`
  - `remoteOrderStatusDisplay`
  - `remoteOrderedAt`
  - `payload`
  - `createdAt`
  - `updatedAt`

## Webhook 수신

- `POST /api/webhooks/sweetbook/orders`
- 인증: JWT 없이 접근 허용, 서명 검증 필수
- 필수 헤더:
  - `X-Webhook-Signature`
  - `X-Webhook-Timestamp` (epoch seconds)
  - `X-Webhook-Event`
  - `X-Webhook-Delivery`
- 서명 검증:
  - 검증 문자열: `{timestamp}.{rawBody}`
  - 알고리즘: `HMAC-SHA256`
  - 기대 포맷: `sha256={hex}`
- timestamp 검증:
  - 현재 시각과 허용 오차(`app.sweetbook.webhook-timestamp-tolerance`) 이내여야 함
- 처리 규칙:
  - 같은 `X-Webhook-Delivery`는 중복 처리하지 않음
  - 상태 역전이(낮은 단계 코드)는 무시
  - `order.restored`는 `CANCELLED -> CREATED` 복구를 허용

## 원격 상태코드 매핑

- `20` PAID -> `CREATED`
- `25` PDF_READY -> `CREATED`
- `30` CONFIRMED -> `CREATED`
- `40` IN_PRODUCTION -> `CREATED`
- `50` PRODUCTION_COMPLETE -> `CREATED`
- `60` SHIPPED -> `CREATED`
- `70` DELIVERED -> `COMPLETED`
- `80/81` CANCELLED/CANCELLED_REFUND -> `CANCELLED`
- `90` ERROR -> `FAILED`

## 주요 에러 코드(연동 시 참고)

- `AUTH_001`: 인증 필요 (Webhook 서명/타임스탬프 오류 포함)
- `AUTH_002`: 권한 없음
- `ALBUM_001`: 앨범 없음
- `COMMON_001`: 잘못된 요청
- `SWEETBOOK_001`: Sweetbook API 호출 실패

