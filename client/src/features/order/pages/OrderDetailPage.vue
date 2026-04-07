<script setup lang="ts">
import { onMounted, computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useOrderStore } from '../store';
import { OrderStatus } from '../types';

const route = useRoute();
const router = useRouter();
const orderStore = useOrderStore();

const albumId = computed(() => Number(route.params.albumId));
const orderId = computed(() => Number(route.params.orderId));

const isEditingShipping = ref(false);
const shippingForm = ref({
  recipientName: '',
  phoneNumber: '',
  postalCode: '',
  address: '',
  addressDetail: '',
});

onMounted(async () => {
  if (albumId.value && orderId.value) {
    await orderStore.fetchOrderDetail(albumId.value, orderId.value);
    // TODO: 백엔드에서 상세 조회 시 배송지 정보를 응답에 포함하는지 확인 필요
    // 현재 OrderResponse 타입에는 배송지 정보가 없으므로 API 컨트랙트 확인 필요
  }
});

const order = computed(() => orderStore.currentOrder);

// 취소 가능 여부: 20(PAID), 25(PDF_READY)
const canCancel = computed(() => {
  const code = order.value?.remoteOrderStatusCode;
  return code === '20' || code === '25';
});

// 배송지 수정 가능 여부: 20(PAID), 25(PDF_READY), 30(CONFIRMED)
const canUpdateShipping = computed(() => {
  const code = order.value?.remoteOrderStatusCode;
  return code === '20' || code === '25' || code === '30';
});

const handleCancel = async () => {
  if (!confirm('주문을 취소하시겠습니까?')) return;
  
  const success = await orderStore.cancelOrder(albumId.value, orderId.value);
  if (success) {
    alert('주문이 취소되었습니다.');
  }
};

const startEditShipping = () => {
  isEditingShipping.ref = true;
  // TODO: 현재 배송지 정보로 폼 초기화 로직 필요
};

const handleUpdateShipping = async () => {
  const success = await orderStore.updateShipping(albumId.value, orderId.value, shippingForm.value);
  if (success) {
    alert('배송지가 수정되었습니다.');
    isEditingShipping.value = false;
  }
};

const goBack = () => {
  router.back();
};

const getStatusLabel = (status: OrderStatus | undefined) => {
  if (!status) return '';
  switch (status) {
    case 'CREATED': return '주문 진행 중';
    case 'COMPLETED': return '배송 완료';
    case 'CANCELLED': return '주문 취소';
    case 'FAILED': return '주문 실패';
    default: return status;
  }
};

const formatDate = (dateString: string | undefined) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
};
</script>

<template>
  <div class="order-detail-page container">
    <header class="page-header">
      <button class="back-button" @click="goBack">
        <span class="icon">←</span> 목록으로
      </button>
      <h1 class="page-title">주문 상세 정보</h1>
    </header>

    <div v-if="orderStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>주문 정보를 불러오는 중입니다...</p>
    </div>

    <div v-else-if="orderStore.error" class="error-state">
      <p class="error-message">{{ orderStore.error }}</p>
      <button class="retry-button" @click="orderStore.fetchOrderDetail(albumId, orderId)">다시 시도</button>
    </div>

    <div v-else-if="order" class="detail-content">
      <section class="info-section status-section">
        <div class="status-header">
          <h2 class="section-title">주문 상태</h2>
          <span :class="['status-badge', `status-${order.status.toLowerCase()}`]">
            {{ getStatusLabel(order.status) }}
          </span>
        </div>
        <div class="status-details">
          <p class="remote-status">{{ order.remoteOrderStatusDisplay }}</p>
          <p class="created-at">주문 일시: {{ formatDate(order.createdAt) }}</p>
          <p v-if="order.lastErrorMessage" class="error-info">{{ order.lastErrorMessage }}</p>
        </div>
      </section>

      <section class="info-section order-id-section">
        <h2 class="section-title">주문 식별 정보</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">주문 번호 (UID)</span>
            <span class="value">{{ order.orderUid }}</span>
          </div>
          <div class="info-item">
            <span class="label">외부 참조 번호</span>
            <span class="value">{{ order.externalRef }}</span>
          </div>
        </div>
      </section>

      <!-- TODO: 배송지 정보 표시 및 수정 UI (API 응답 필드 확인 후 보완 예정) -->
      <section class="info-section action-section">
        <div class="actions">
          <button 
            v-if="canCancel" 
            class="cancel-button"
            @click="handleCancel"
          >
            주문 취소
          </button>
          
          <button 
            v-if="canUpdateShipping" 
            class="edit-button"
            @click="startEditShipping"
          >
            배송지 수정
          </button>
        </div>
        <p v-if="!canCancel && !canUpdateShipping" class="notice">
          현재 상태({{ order.remoteOrderStatusDisplay }})에서는 주문 취소나 배송지 수정이 불가능합니다.
        </p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.order-detail-page {
  padding-top: 40px;
  padding-bottom: 80px;
}

.page-header {
  margin-bottom: 40px;
}

.back-button {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  box-shadow: 0 0 0 1px var(--color-ring-warm);
}

.page-title {
  font-size: 32px;
  margin: 0;
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.info-section {
  background-color: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 32px;
}

.section-title {
  font-size: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--color-border-warm);
  padding-bottom: 12px;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.status-badge {
  font-size: 14px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 20px;
}

.status-created { background-color: #fef3c7; color: #92400e; }
.status-completed { background-color: #d1fae5; color: #065f46; }
.status-cancelled { background-color: #fee2e2; color: #991b1b; }
.status-failed { background-color: #f3f4f6; color: #374151; }

.remote-status {
  font-size: 24px;
  font-weight: 500;
  margin: 16px 0 8px;
}

.created-at {
  color: var(--color-olive-gray);
  font-size: 14px;
}

.error-info {
  margin-top: 16px;
  padding: 12px;
  background-color: #fff1f2;
  color: #be123c;
  border-radius: 8px;
  font-size: 14px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label {
  font-size: 12px;
  color: var(--color-stone-gray);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.value {
  font-size: 18px;
  font-family: var(--font-mono);
}

.action-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.actions {
  display: flex;
  gap: 16px;
}

.cancel-button {
  background-color: var(--color-white);
  color: var(--color-error);
  border: 1px solid var(--color-error);
  padding: 12px 24px;
  font-weight: 500;
}

.cancel-button:hover {
  background-color: #fff1f2;
}

.edit-button {
  background-color: var(--color-terracotta);
  color: var(--color-white);
  padding: 12px 24px;
  font-weight: 500;
}

.notice {
  font-size: 14px;
  color: var(--color-olive-gray);
  font-style: italic;
}

.loading-state, .error-state {
  padding: 80px 0;
  text-align: center;
  background-color: var(--color-ivory);
  border-radius: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border-warm);
  border-top-color: var(--color-terracotta);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
