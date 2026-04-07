# 크레딧 API 계약서 (연동 고정)

## 공통
- Base Path: `/api`
- 인증: `Authorization: Bearer {accessToken}` 필수
- 응답 래퍼: `ApiResponse<T>`

## 1) 크레딧 잔액 조회
- `GET /api/credits`
- Response `data` (`CreditBalanceResponse`)
  - `accountUid`: 계정 UID
  - `balance`: 현재 잔액(원)
  - `currency`: 통화(`KRW`)
  - `env`: 환경(`test` 또는 `live`)
  - `createdAt`: 계정 생성 시각
  - `updatedAt`: 잔액 마지막 변경 시각

### 예시
```json
{
  "success": true,
  "data": {
    "accountUid": "acc_abc123xyz",
    "balance": 100000,
    "currency": "KRW",
    "env": "test",
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-03-01T10:00:00"
  },
  "error": null,
  "timestamp": "2026-04-07T16:00:00+09:00"
}
```

## 2) Sandbox 크레딧 충전
- `POST /api/credits/sandbox/charge`
- Request `ChargeCreditRequest`
  - `amount` (필수, 양수)
  - `memo` (선택, 최대 200자)
  - `idempotencyKey` (선택, 최대 120자)

- Response `data` (`ChargeCreditResponse`)
  - `transactionUid`
  - `amount`
  - `balanceAfter`
  - `currency`

### 예시
```json
{
  "success": true,
  "data": {
    "transactionUid": "tx_sandbox_abc123",
    "amount": 100000,
    "balanceAfter": 200000,
    "currency": "KRW"
  },
  "error": null,
  "timestamp": "2026-04-07T16:01:00+09:00"
}
```

## 참고
- 충전 API는 Sweetbook `POST /v1/credits/sandbox/charge`를 호출합니다.
- 외부 연동 실패 시 `SWEETBOOK_001`로 매핑됩니다.
