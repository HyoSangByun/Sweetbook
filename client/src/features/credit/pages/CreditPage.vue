<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useCreditStore } from '../store';

const router = useRouter();
const creditStore = useCreditStore();

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

const goBack = () => {
  router.push({ name: 'dashboard' });
};
</script>

<template>
  <div class="credit-page container">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">Back to dashboard</button>
      <h1 class="page-title">Credits</h1>
    </header>

    <section class="card balance-card">
      <div class="card-head">
        <h2>Current balance</h2>
        <button type="button" class="refresh-button" :disabled="creditStore.isFetchingBalance" @click="creditStore.fetchBalance">
          {{ creditStore.isFetchingBalance ? 'Refreshing...' : 'Refresh' }}
        </button>
      </div>

      <div v-if="creditStore.isFetchingBalance && !creditStore.balance" class="state-block">Loading balance...</div>
      <div v-else-if="creditStore.fetchError" class="state-block error">{{ creditStore.fetchError }}</div>
      <div v-else-if="creditStore.balance" class="balance-content">
        <p class="balance-amount">{{ formattedBalance }}</p>
        <p class="meta">Environment: {{ environmentLabel }}</p>
      </div>
      <div v-else class="state-block">No balance information.</div>
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

.error {
  color: var(--color-error);
}
</style>
