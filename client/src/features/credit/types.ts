export interface CreditBalanceResponse {
  balance: number;
  currency: 'KRW' | string;
  env: 'test' | 'live' | string;
}
