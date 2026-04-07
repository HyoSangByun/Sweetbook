<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useCreditStore } from '../store';

const router = useRouter();
const creditStore = useCreditStore();

const form = reactive({
  amount: '100000',
  memo: '',
  idempotencyKey: '',
});

const formError = computed(() => {
  const amount = Number(form.amount);

  if (!Number.isInteger(amount) || amount <= 0) {
    return 'Amount must be a positive integer.';
  }

  if (form.memo.length > 200) {
    return 'Memo must be 200 characters or less.';
  }

  if (form.idempotencyKey.length > 120) {
    return 'Idempotency key must be 120 characters or less.';
  }

  return null;
});

const chargeSuccessMessage = computed(() => {
  if (!creditStore.latestCharge) return null;
  return `Charged ${formatAmount(creditStore.latestCharge.amount)} (${creditStore.latestCharge.transactionUid})`;
});

onMounted(async () => {
  await creditStore.fetchBalance();
});

const formatAmount = (value: number) => {
  return new Intl.NumberFormat('ko-KR').format(value);
};

const formatDateTime = (value: string) => {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).format(new Date(value));
};

const handleCharge = async () => {
  if (creditStore.isCharging || formError.value) return;

  await creditStore.charge({
    amount: Number(form.amount),
    memo: form.memo.trim() || undefined,
    idempotencyKey: form.idempotencyKey.trim() || undefined,
  });
};

const goBack = () => {
  router.push({ name: 'dashboard' });
};
</script>

<template>
  <div class="credit-page container">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">Back</button>
      <h1 class="page-title">Credits</h1>
    </header>

    <section class="card balance-card">
      <div class="card-head">
        <h2>Balance</h2>
        <button type="button" class="refresh-button" :disabled="creditStore.isFetchingBalance" @click="creditStore.fetchBalance">
          {{ creditStore.isFetchingBalance ? 'Refreshing...' : 'Refresh' }}
        </button>
      </div>

      <div v-if="creditStore.isFetchingBalance && !creditStore.balance" class="state-block">Loading balance...</div>
      <div v-else-if="creditStore.fetchError" class="state-block error">{{ creditStore.fetchError }}</div>
      <div v-else-if="creditStore.balance" class="balance-content">
        <p class="balance-amount">{{ formatAmount(creditStore.balance.balance) }} {{ creditStore.balance.currency }}</p>
        <p class="meta">Account: {{ creditStore.balance.accountUid }}</p>
        <p class="meta">Environment: {{ creditStore.balance.env }}</p>
        <p class="meta">Updated: {{ formatDateTime(creditStore.balance.updatedAt) }}</p>
      </div>
    </section>

    <section class="card charge-card">
      <h2>Sandbox Charge</h2>
      <form class="charge-form" @submit.prevent="handleCharge">
        <label class="field">
          <span>Amount</span>
          <input v-model="form.amount" type="number" min="1" step="1" required />
        </label>

        <label class="field">
          <span>Memo (optional, max 200)</span>
          <input v-model="form.memo" type="text" maxlength="200" />
        </label>

        <label class="field">
          <span>Idempotency Key (optional, max 120)</span>
          <input v-model="form.idempotencyKey" type="text" maxlength="120" />
        </label>

        <p v-if="formError" class="error">{{ formError }}</p>
        <p v-if="creditStore.chargeError" class="error">{{ creditStore.chargeError }}</p>
        <p v-if="chargeSuccessMessage" class="success">{{ chargeSuccessMessage }}</p>

        <button type="submit" class="charge-button" :disabled="creditStore.isCharging || !!formError">
          {{ creditStore.isCharging ? 'Charging...' : 'Charge' }}
        </button>
      </form>
    </section>

    <section class="card history-card">
      <h2>Transaction History</h2>
      <p class="meta">
        TODO(contract): docs/CREDITS_API_CONTRACT.md currently does not define a transaction history endpoint,
        so list UI is pending contract update.
      </p>
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

.charge-form {
  display: grid;
  gap: 12px;
}

.field {
  display: grid;
  gap: 6px;
}

.error {
  margin: 0;
  color: var(--color-error);
}

.success {
  margin: 0;
  color: #2c6d47;
}

.charge-button {
  justify-self: flex-start;
  background: var(--color-terracotta);
  color: var(--color-white);
  padding: 10px 16px;
}

.charge-button:disabled,
.refresh-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
