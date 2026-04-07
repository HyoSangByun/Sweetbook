import { defineStore } from 'pinia';
import { creditApi } from '../api/creditApi';
import type { CreditBalanceResponse } from '../types';

interface ApiLikeError {
  code?: string;
  message?: string;
  details?: {
    status?: number;
  };
}

const toErrorMessage = (error: unknown, fallback: string) => {
  const apiError = error as ApiLikeError;
  const parts: string[] = [];

  if (typeof apiError?.details?.status === 'number') {
    parts.push(`HTTP ${apiError.details.status}`);
  }

  if (apiError?.code) {
    parts.push(apiError.code);
  }

  if (apiError?.message) {
    parts.push(apiError.message);
  }

  return parts.length > 0 ? parts.join(' | ') : fallback;
};

export const useCreditStore = defineStore('credit', {
  state: () => ({
    balance: null as CreditBalanceResponse | null,
    isFetchingBalance: false,
    fetchError: null as string | null,
  }),

  actions: {
    async fetchBalance() {
      if (this.isFetchingBalance) return;

      this.isFetchingBalance = true;
      this.fetchError = null;

      try {
        this.balance = await creditApi.getBalance();
      } catch (error) {
        this.fetchError = toErrorMessage(error, '크레딧 잔액을 불러오지 못했습니다.');
      } finally {
        this.isFetchingBalance = false;
      }
    },
  },
});
