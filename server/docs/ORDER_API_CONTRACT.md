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
  - `orderUid` (원격 주문 생성 전/실패 시 null 가능)
  - `externalRef`
  - `status` (`REQUESTED`, `CREATED`, `COMPLETED`, `CANCELLED`, `FAILED`)
  - `lastErrorMessage`
  - `remoteOrderStatusCode` (웹훅 동기화 전에는 null)
  - `remoteOrderStatusDisplay` (웹훅 동기화 전에는 null)
  - `remoteOrderedAt` (웹훅 동기화 전에는 null)
  - `createdAt`

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
  - `payload` (백엔드 저장 요청 페이로드)
  - `createdAt`
  - `updatedAt`

## Webhook 수신

- `POST /api/webhooks/sweetbook/orders`
- 인증: JWT 없이 접근 허용, 서명 검증 필수
- 필수 헤더:
  - `X-Webhook-Signature`
  - `X-Webhook-Timestamp`
  - `X-Webhook-Event`
  - `X-Webhook-Delivery`
- 서명 검증:
  - 검증 문자열: `{timestamp}.{rawBody}`
  - 알고리즘: `HMAC-SHA256`
  - 기대 포맷: `sha256={hex}`
- 처리 규칙:
  - 같은 `X-Webhook-Delivery`는 중복 처리하지 않음
  - 상태 역전이(낮은 단계 코드)는 무시
  - 원격 상태코드 매핑:
    - `20~60` -> `CREATED`
    - `70` -> `COMPLETED`
    - `80/81` -> `CANCELLED`
    - `90` -> `FAILED`

## 주요 에러 코드(연동 시 참고)

- `AUTH_001`: 인증 필요 (웹훅 서명 오류 포함)
- `AUTH_002`: 권한 없음
- `ALBUM_001`: 앨범 없음
- `COMMON_001`: 잘못된 요청
- `SWEETBOOK_001`: Sweetbook API 호출 실패

