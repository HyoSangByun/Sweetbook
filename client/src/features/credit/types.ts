export interface CreditBalanceResponse {
  balance: number;
  currency: 'KRW' | string;
  env: 'test' | 'live' | string;
}

export interface ChargeCreditResponse {
  transactionUid: string;
  amount: number;
  balanceAfter: number;
  currency: 'KRW' | string;
}
