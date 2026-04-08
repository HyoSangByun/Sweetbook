<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useCreditStore } from '../store';

const router = useRouter();
const creditStore = useCreditStore();
const chargeAmount = ref<number>(10000);

onMounted(async () => {
  await creditStore.fetchBalance();
});

const formattedBalance = computed(() => {
  if (!creditStore.balance) {
    return '-';
  }

  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(creditStore.balance.balance);
});

const environmentLabel = computed(() => {
  const env = creditStore.balance?.env;
  if (!env) return '-';
  return env === 'live' ? 'Live' : 'Sandbox';
});

const chargeValidationMessage = computed(() => {
  if (chargeAmount.value == null || Number.isNaN(chargeAmount.value)) {
    return '충전 금액을 입력해 주세요.';
  }
  if (chargeAmount.value <= 0) {
    return '충전 금액은 1 이상이어야 합니다.';
  }
  return null;
});

const handleCharge = async () => {
  if (chargeValidationMessage.value) return;
  await creditStore.chargeSandbox(Number(chargeAmount.value));
  if (creditStore.chargeSuccess) {
    window.alert(creditStore.chargeSuccess);
    window.location.reload();
  }
};

const goBack = () => {
  router.push({ name: 'dashboard' });
};
</script>

<template>
  <div class="credit-page container">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">홈으로</button>
      <h1 class="page-title">크레딧</h1>
    </header>

    <section class="card balance-card">
      <div class="card-head">
        <h2>현재 잔액</h2>
        <button type="button" class="refresh-button" :disabled="creditStore.isFetchingBalance" @click="creditStore.fetchBalance">
          {{ creditStore.isFetchingBalance ? '새로고침 중...' : '새로고침' }}
        </button>
      </div>

      <div v-if="creditStore.isFetchingBalance && !creditStore.balance" class="state-block">잔액을 불러오는 중...</div>
      <div v-else-if="creditStore.fetchError" class="state-block error">{{ creditStore.fetchError }}</div>
      <div v-else-if="creditStore.balance" class="balance-content">
        <p class="balance-amount">{{ formattedBalance }}</p>
        <p class="meta">환경: {{ environmentLabel }}</p>
      </div>
      <div v-else class="state-block">잔액 정보가 없습니다.</div>
    </section>

    <section class="card">
      <div class="card-head">
        <h2>샌드박스 충전</h2>
      </div>

      <label class="field">
        <span>충전 금액 (amount)</span>
        <input v-model.number="chargeAmount" type="number" min="1" step="1" />
      </label>

      <button
        type="button"
        class="charge-button"
        :disabled="!!chargeValidationMessage || creditStore.isCharging"
        @click="handleCharge"
      >
        {{ creditStore.isCharging ? '충전 중...' : '충전하기' }}
      </button>

      <p v-if="chargeValidationMessage" class="state-block error">{{ chargeValidationMessage }}</p>
      <p v-if="creditStore.chargeError" class="state-block error">{{ creditStore.chargeError }}</p>
      <p v-if="creditStore.chargeSuccess" class="state-block success">{{ creditStore.chargeSuccess }}</p>
    </section>
  </div>
</template>

<style scoped>
.credit-page {
  padding-top: 36px;
  padding-bottom: 72px;
  display: grid;
  gap: 16px;
}

.page-header {
  margin-bottom: 4px;
}

.back-button {
  margin-bottom: 10px;
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
  box-shadow: 0 0 0 1px var(--color-ring-warm);
}

.page-title {
  margin: 0;
  font-size: 36px;
}

.card {
  background: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  border-radius: 16px;
  padding: 20px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.refresh-button {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
}

.refresh-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.state-block {
  margin-top: 12px;
  color: var(--color-olive-gray);
}

.balance-amount {
  font-family: var(--font-serif);
  font-size: 40px;
  margin: 12px 0;
}

.meta {
  margin: 4px 0;
  color: var(--color-olive-gray);
}

.field {
  display: grid;
  gap: 6px;
  margin-top: 10px;
}

.charge-button {
  margin-top: 12px;
  background: var(--color-terracotta);
  color: var(--color-white);
  padding: 10px 14px;
}

.charge-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error {
  color: var(--color-error);
}

.success {
  color: #2c6d47;
}
</style>
