export interface CreditBalanceResponse {
  accountUid: string;
  balance: number;
  currency: 'KRW' | string;
  env: 'test' | 'live' | string;
  createdAt: string;
  updatedAt: string;
}

export interface ChargeCreditRequest {
  amount: number;
  memo?: string;
  idempotencyKey?: string;
}

export interface ChargeCreditResponse {
  transactionUid: string;
  amount: number;
  balanceAfter: number;
  currency: 'KRW' | string;
}
