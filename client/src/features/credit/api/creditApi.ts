import client from '../../../shared/api/client';
import type { ChargeCreditResponse, CreditBalanceResponse } from '../types';

export const creditApi = {
  getBalance: async () => {
    return client.get<CreditBalanceResponse>('/credits');
  },
  chargeSandbox: async (amount: number) => {
    return client.post<ChargeCreditResponse>('/credits/sandbox/charge', { amount });
  },
};
