import { defineStore } from 'pinia';
import { creditApi } from '../api/creditApi';
import type { CreditBalanceResponse } from '../types';
import { toErrorMessage } from '../../../shared/utils/errorMessage';

export const useCreditStore = defineStore('credit', {
  state: () => ({
    balance: null as CreditBalanceResponse | null,
    isFetchingBalance: false,
    isCharging: false,
    fetchError: null as string | null,
    chargeError: null as string | null,
    chargeSuccess: null as string | null,
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

    async chargeSandbox(amount: number) {
      if (this.isCharging) return;

      this.isCharging = true;
      this.chargeError = null;
      this.chargeSuccess = null;

      try {
        const response = await creditApi.chargeSandbox(amount);
        this.chargeSuccess = `충전 완료: ${response.amount.toLocaleString('ko-KR')} ${response.currency}`;
        await this.fetchBalance();
      } catch (error) {
        this.chargeError = toErrorMessage(error, '크레딧 충전에 실패했습니다.');
      } finally {
        this.isCharging = false;
      }
    },
  },
});
