import { defineStore } from 'pinia';
import { creditApi } from '../api/creditApi';
import type { ChargeCreditRequest, ChargeCreditResponse, CreditBalanceResponse } from '../types';

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
    latestCharge: null as ChargeCreditResponse | null,

    isFetchingBalance: false,
    isCharging: false,

    fetchError: null as string | null,
    chargeError: null as string | null,
  }),

  actions: {
    async fetchBalance() {
      if (this.isFetchingBalance) return;

      this.isFetchingBalance = true;
      this.fetchError = null;

      try {
        this.balance = await creditApi.getBalance();
      } catch (error) {
        this.fetchError = toErrorMessage(error, 'Failed to load credit balance.');
      } finally {
        this.isFetchingBalance = false;
      }
    },

    async charge(payload: ChargeCreditRequest) {
      if (this.isCharging) return null;

      this.isCharging = true;
      this.chargeError = null;

      try {
        const response = await creditApi.chargeSandbox(payload);
        this.latestCharge = response;

        // Server is source of truth: refresh after mutation.
        await this.fetchBalance();

        return response;
      } catch (error) {
        this.chargeError = toErrorMessage(error, 'Failed to charge credits.');
        return null;
      } finally {
        this.isCharging = false;
      }
    },
  },
});
