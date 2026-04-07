import client from '../../../shared/api/client';
import type { CreditBalanceResponse } from '../types';

export const creditApi = {
  getBalance: async () => {
    return client.get<CreditBalanceResponse>('/credits');
  },
};
