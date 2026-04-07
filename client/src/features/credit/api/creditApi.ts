import client from '../../../shared/api/client';
import type {
  ChargeCreditRequest,
  ChargeCreditResponse,
  CreditBalanceResponse,
} from '../types';

export const creditApi = {
  getBalance: async () => {
    return client.get<CreditBalanceResponse>('/credits');
  },

  chargeSandbox: async (payload: ChargeCreditRequest) => {
    return client.post<ChargeCreditResponse>('/credits/sandbox/charge', payload);
  },
};
