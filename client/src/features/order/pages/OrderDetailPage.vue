<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ShippingAddressModal from '../components/ShippingAddressModal.vue';
import { useOrderStore } from '../store';
import type { OrderStatus, ShippingUpdateRequest } from '../types';

const route = useRoute();
const router = useRouter();
const orderStore = useOrderStore();

const albumId = computed(() => Number(route.params.albumId));
const orderId = computed(() => Number(route.params.orderId));

const isShippingModalOpen = ref(false);
const successMessage = ref<string | null>(null);

const order = computed(() => orderStore.currentOrder);

const normalizedRemoteCode = computed(() => {
  return String(order.value?.remoteOrderStatusCode ?? '');
});

const canCancel = computed(() => {
  return normalizedRemoteCode.value === '20' || normalizedRemoteCode.value === '25';
});

const canUpdateShipping = computed(() => {
  return ['20', '25', '30'].includes(normalizedRemoteCode.value);
});

const loadOrder = async () => {
  if (!albumId.value || !orderId.value) {
    return;
  }

  await orderStore.fetchOrderDetail(albumId.value, orderId.value);
};

onMounted(async () => {
  await loadOrder();
});

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(dateString));
};

const getStatusLabel = (status: OrderStatus) => {
  switch (status) {
    case 'CREATED':
      return '진행 중';
    case 'COMPLETED':
      return '완료';
    case 'CANCELLED':
      return '취소';
    case 'FAILED':
      return '실패';
    default:
      return status;
  }
};

const goBack = () => {
  router.push({ name: 'order-list', params: { albumId: albumId.value } });
};

const handleCancel = async () => {
  if (!order.value || orderStore.isCancelling) {
    return;
  }

  if (!window.confirm('주문을 취소하시겠습니까?')) {
    return;
  }

  successMessage.value = null;

  const success = await orderStore.cancelOrder(albumId.value, orderId.value);
  if (success) {
    successMessage.value = '주문 취소가 완료되었습니다.';
  }
};

const handleSubmitShipping = async (payload: ShippingUpdateRequest) => {
  if (!order.value || orderStore.isUpdatingShipping) {
    return;
  }

  successMessage.value = null;

  const success = await orderStore.updateShipping(albumId.value, orderId.value, payload);
  if (success) {
    successMessage.value = '배송지 수정이 완료되었습니다.';
    isShippingModalOpen.value = false;
  }
};
</script>

<template>
  <div class="order-detail-page container">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">목록으로</button>
      <h1 class="page-title">주문 상세</h1>
    </header>

    <div v-if="orderStore.isFetchingDetail" class="loading-state">
      <div class="spinner"></div>
      <p>주문 상세 정보를 불러오는 중입니다...</p>
    </div>

    <div v-else-if="orderStore.detailError" class="error-state">
      <p class="error-message">{{ orderStore.detailError }}</p>
      <button class="retry-button" type="button" @click="loadOrder">다시 시도</button>
    </div>

    <div v-else-if="order" class="detail-content">
      <section class="info-section">
        <div class="title-row">
          <h2 class="section-title">주문 상태</h2>
          <span :class="['status-badge', `status-${order.status.toLowerCase()}`]">{{ getStatusLabel(order.status) }}</span>
        </div>

        <p class="status-line">원격 상태: {{ order.remoteOrderStatusDisplay }} ({{ order.remoteOrderStatusCode }})</p>
        <p class="meta-line">주문 시간: {{ formatDate(order.createdAt) }}</p>
        <p class="meta-line">원격 주문 시간: {{ formatDate(order.remoteOrderedAt) }}</p>
        <p v-if="order.lastErrorMessage" class="error-message">{{ order.lastErrorMessage }}</p>
        <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
      </section>

      <section class="info-section">
        <h2 class="section-title">주문 식별 정보</h2>
        <div class="kv-grid">
          <div>
            <p class="kv-label">orderUid</p>
            <p class="kv-value">{{ order.orderUid }}</p>
          </div>
          <div>
            <p class="kv-label">externalRef</p>
            <p class="kv-value">{{ order.externalRef }}</p>
          </div>
        </div>
      </section>

      <section class="info-section">
        <h2 class="section-title">주문 작업</h2>
        <div class="actions">
          <button
            class="secondary-action"
            type="button"
            :disabled="!canCancel || orderStore.isCancelling"
            @click="handleCancel"
          >
            {{ orderStore.isCancelling ? '취소 처리 중...' : '주문 취소' }}
          </button>

          <button
            class="primary-action"
            type="button"
            :disabled="!canUpdateShipping || orderStore.isUpdatingShipping"
            @click="isShippingModalOpen = true"
          >
            배송지 수정
          </button>
        </div>

        <p class="notice-text" v-if="!canCancel && !canUpdateShipping">
          현재 상태에서는 주문 취소 또는 배송지 수정이 불가능합니다.
        </p>
        <p class="notice-text" v-else>
          취소 가능 코드: 20, 25 / 배송지 수정 가능 코드: 20, 25, 30
        </p>

        <p v-if="orderStore.cancelError" class="error-message">{{ orderStore.cancelError }}</p>
        <p v-if="orderStore.updateShippingError" class="error-message">{{ orderStore.updateShippingError }}</p>
      </section>
    </div>

    <ShippingAddressModal
      :open="isShippingModalOpen"
      :pending="orderStore.isUpdatingShipping"
      :error-message="orderStore.updateShippingError"
      @close="isShippingModalOpen = false"
      @submit="handleSubmitShipping"
    />
  </div>
</template>

<style scoped>
.order-detail-page {
  padding-top: 36px;
  padding-bottom: 72px;
}

.page-header {
  margin-bottom: 24px;
}

.back-button {
  margin-bottom: 12px;
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
  box-shadow: 0 0 0 1px var(--color-ring-warm);
}

.page-title {
  font-size: 36px;
  margin: 0;
}

.detail-content {
  display: grid;
  gap: 16px;
}

.info-section {
  background: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 20px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.section-title {
  margin: 0;
  font-size: 26px;
}

.status-badge {
  font-size: 12px;
  border-radius: 999px;
  padding: 5px 10px;
  font-weight: 600;
}

.status-created {
  background: #fae8d7;
  color: #89492d;
}

.status-completed {
  background: #d8f0dd;
  color: #1f6132;
}

.status-cancelled {
  background: #f7dddb;
  color: #8d2e2a;
}

.status-failed {
  background: #ece9e0;
  color: #4d4c48;
}

.status-line,
.meta-line,
.notice-text {
  margin: 6px 0;
  color: var(--color-olive-gray);
}

.kv-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.kv-label {
  margin: 0;
  font-size: 12px;
  color: var(--color-stone-gray);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.kv-value {
  margin: 6px 0 0;
  font-family: var(--font-mono);
  font-size: 15px;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.secondary-action {
  background: var(--color-white);
  border: 1px solid var(--color-error);
  color: var(--color-error);
  padding: 10px 14px;
}

.primary-action {
  background: var(--color-terracotta);
  color: var(--color-ivory);
  padding: 10px 14px;
}

.secondary-action:disabled,
.primary-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loading-state,
.error-state {
  text-align: center;
  background: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 48px 20px;
}

.error-message {
  margin: 8px 0 0;
  color: var(--color-error);
}

.success-message {
  margin: 8px 0 0;
  color: #2c6d47;
}

.retry-button {
  margin-top: 12px;
  background: var(--color-terracotta);
  color: var(--color-ivory);
  padding: 8px 12px;
}

.spinner {
  width: 30px;
  height: 30px;
  border: 3px solid var(--color-border-warm);
  border-top-color: var(--color-terracotta);
  border-radius: 50%;
  margin: 0 auto 10px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .page-title {
    font-size: 30px;
  }

  .kv-grid {
    grid-template-columns: 1fr;
  }
}
</style>

