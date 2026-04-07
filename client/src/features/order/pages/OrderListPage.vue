<script setup lang="ts">
import { onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useOrderStore } from '../store';
import { OrderStatus } from '../types';

const route = useRoute();
const router = useRouter();
const orderStore = useOrderStore();

const albumId = computed(() => Number(route.params.albumId));

onMounted(async () => {
  if (albumId.value) {
    await orderStore.fetchOrders(albumId.value);
  }
});

const getStatusLabel = (status: OrderStatus) => {
  switch (status) {
    case 'CREATED': return '주문 진행 중';
    case 'COMPLETED': return '배송 완료';
    case 'CANCELLED': return '주문 취소';
    case 'FAILED': return '주문 실패';
    default: return status;
  }
};

const getStatusClass = (status: OrderStatus) => {
  switch (status) {
    case 'CREATED': return 'status-created';
    case 'COMPLETED': return 'status-completed';
    case 'CANCELLED': return 'status-cancelled';
    case 'FAILED': return 'status-failed';
    default: return '';
  }
};

const formatDate = (dateString: string) => {
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

const goToDetail = (orderId: number) => {
  router.push(`/albums/${albumId.value}/orders/${orderId}`);
};

const goBack = () => {
  router.back();
};
</script>

<template>
  <div class="order-list-page container">
    <header class="page-header">
      <button class="back-button" @click="goBack">
        <span class="icon">←</span> 뒤로 가기
      </button>
      <h1 class="page-title">주문 내역</h1>
    </header>

    <div v-if="orderStore.loading" class="loading-state">
      <div class="spinner"></div>
      <p>주문 내역을 불러오는 중입니다...</p>
    </div>

    <div v-else-if="orderStore.error" class="error-state">
      <p class="error-message">{{ orderStore.error }}</p>
      <button class="retry-button" @click="orderStore.fetchOrders(albumId)">다시 시도</button>
    </div>

    <div v-else-if="orderStore.orders.length === 0" class="empty-state">
      <p>주문 내역이 없습니다.</p>
    </div>

    <div v-else class="order-grid">
      <div 
        v-for="order in orderStore.orders" 
        :key="order.orderId" 
        class="order-card"
        @click="goToDetail(order.orderId)"
      >
        <div class="order-card-header">
          <span class="order-date">{{ formatDate(order.createdAt) }}</span>
          <span :class="['status-badge', getStatusClass(order.status)]">
            {{ getStatusLabel(order.status) }}
          </span>
        </div>
        
        <div class="order-card-body">
          <div class="order-info">
            <span class="label">주문 번호</span>
            <span class="value">{{ order.orderUid }}</span>
          </div>
          <div class="order-info">
            <span class="label">상품 정보</span>
            <span class="value">{{ order.remoteOrderStatusDisplay || '포토북' }}</span>
          </div>
        </div>

        <div class="order-card-footer">
          <span class="view-detail">상세 보기</span>
          <span class="arrow">→</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.order-list-page {
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

.loading-state, .error-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  background-color: var(--color-ivory);
  border-radius: 16px;
  border: 1px solid var(--color-border-cream);
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 24px;
}

.order-card {
  background-color: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card:hover {
  transform: translateY(-4px);
  box-shadow: rgba(0, 0, 0, 0.05) 0px 4px 24px;
}

.order-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-date {
  font-size: 14px;
  color: var(--color-olive-gray);
}

.status-badge {
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 20px;
}

.status-created {
  background-color: #fef3c7;
  color: #92400e;
}

.status-completed {
  background-color: #d1fae5;
  color: #065f46;
}

.status-cancelled {
  background-color: #fee2e2;
  color: #991b1b;
}

.status-failed {
  background-color: #f3f4f6;
  color: #374151;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 12px;
  color: var(--color-stone-gray);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.value {
  font-size: 16px;
  font-weight: 500;
}

.order-card-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-cream);
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--color-terracotta);
  font-weight: 500;
  font-size: 14px;
}

.retry-button {
  margin-top: 16px;
  background-color: var(--color-terracotta);
  color: var(--color-white);
  padding: 10px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border-warm);
  border-top-color: var(--color-terracotta);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
