<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAlbumStore } from '../../album/store';
import * as albumApi from '../../album/api/albumApi';
import { useOrderStore } from '../store';
import type { OrderRequest, OrderStatus } from '../types';

const route = useRoute();
const router = useRouter();
const orderStore = useOrderStore();
const albumStore = useAlbumStore();

const albumId = computed(() => Number(route.params.albumId));

const createForm = reactive({
  quantity: '1',
  externalRef: '',
  externalUserId: '',
  recipientName: '',
  phoneNumber: '',
  postalCode: '',
  address: '',
  addressDetail: '',
});

const createValidationError = ref<string | null>(null);
const createSuccessMessage = ref<string | null>(null);

const availableBooks = ref<Array<{ bookUid: string; title?: string; status?: number }>>([]);
const selectedBookUid = ref<string | null>(null);
const booksLoadError = ref<string | null>(null);

const loadPage = async () => {
  if (!albumId.value) {
    return;
  }

  await Promise.all([
    orderStore.fetchOrders(albumId.value),
    albumStore.fetchAlbum(albumId.value),
  ]);
  await loadBooks();
};

onMounted(async () => {
  const bookUidFromQuery = typeof route.query.bookUid === 'string' ? route.query.bookUid : null;
  if (bookUidFromQuery) {
    selectedBookUid.value = bookUidFromQuery;
  }
  await loadPage();
});

const loadBooks = async () => {
  if (!albumId.value) return;
  booksLoadError.value = null;
  try {
    const books = await albumApi.getAlbumBooks(albumId.value);
    availableBooks.value = books.map((book: any) => ({
      bookUid: String(book.bookUid),
      title: book.title ? String(book.title) : undefined,
      status: typeof book.status === 'number' ? book.status : undefined,
    }));
    if (!selectedBookUid.value && availableBooks.value.length > 0) {
      selectedBookUid.value = availableBooks.value[0].bookUid;
    }
  } catch (error: any) {
    booksLoadError.value = error?.message || '책 목록을 불러오지 못했습니다.';
  }
};

const formatDate = (dateString: string) => {
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

const getStatusClass = (status: OrderStatus) => {
  switch (status) {
    case 'CREATED':
      return 'status-created';
    case 'COMPLETED':
      return 'status-completed';
    case 'CANCELLED':
      return 'status-cancelled';
    case 'FAILED':
      return 'status-failed';
    default:
      return '';
  }
};

const validateCreateForm = () => {
  const phonePattern = /^\d{2,3}-?\d{3,4}-?\d{4}$/;
  const postalPattern = /^\d{5}$/;
  const quantity = Number(createForm.quantity);

  if (!selectedBookUid.value) {
    return 'bookUid가 없어 주문 생성이 불가능합니다.';
  }

  if (!Number.isInteger(quantity) || quantity <= 0) {
    return '수량은 1 이상의 정수여야 합니다.';
  }

  if (!createForm.externalRef.trim()) {
    return 'externalRef를 입력해 주세요.';
  }

  if (!createForm.externalUserId.trim()) {
    return 'externalUserId를 입력해 주세요.';
  }

  if (!createForm.recipientName.trim()) {
    return '수령인 이름을 입력해 주세요.';
  }

  if (!phonePattern.test(createForm.phoneNumber.trim())) {
    return '전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)';
  }

  if (!postalPattern.test(createForm.postalCode.trim())) {
    return '우편번호는 5자리 숫자여야 합니다.';
  }

  if (!createForm.address.trim()) {
    return '기본 주소를 입력해 주세요.';
  }

  if (!createForm.addressDetail.trim()) {
    return '상세 주소를 입력해 주세요.';
  }

  return null;
};

const handleCreateOrder = async () => {
  if (!albumId.value || orderStore.isCreating) {
    return;
  }

  createSuccessMessage.value = null;
  createValidationError.value = validateCreateForm();

  if (createValidationError.value || !selectedBookUid.value) {
    return;
  }

  const payload: OrderRequest = {
    items: [
      {
        bookUid: selectedBookUid.value,
        quantity: Number(createForm.quantity),
      },
    ],
    shipping: {
      recipientName: createForm.recipientName.trim(),
      recipientPhone: createForm.phoneNumber.trim(),
      postalCode: createForm.postalCode.trim(),
      address1: createForm.address.trim(),
      address2: createForm.addressDetail.trim(),
    },
    externalRef: createForm.externalRef.trim(),
    externalUserId: createForm.externalUserId.trim(),
  };

  const created = await orderStore.createOrder(albumId.value, payload);

  if (created) {
    createSuccessMessage.value = `주문 생성 완료: ${created.orderUid}`;
    router.push({
      name: 'order-detail',
      params: { albumId: albumId.value, orderId: created.orderId },
    });
  }
};

const goToDetail = (orderId: number) => {
  router.push({ name: 'order-detail', params: { albumId: albumId.value, orderId } });
};

const goBack = () => {
  router.push({ name: 'album-detail', params: { id: albumId.value } });
};
</script>

<template>
  <div class="order-list-page container">
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">앨범으로</button>
      <h1 class="page-title">주문</h1>
    </header>

    <section class="create-section">
      <div class="section-header">
        <h2 class="section-title">주문 생성</h2>
        <p class="section-subtitle">ORDER_API_CONTRACT.md 기준 필드만 사용합니다.</p>
      </div>

      <form class="create-form" @submit.prevent="handleCreateOrder">
        <div class="field-grid">
          <label class="field">
            <span>bookUid 선택</span>
            <select v-model="selectedBookUid">
              <option :value="null" disabled>주문할 bookUid 선택</option>
              <option v-for="book in availableBooks" :key="book.bookUid" :value="book.bookUid">
                {{ book.title || '(제목 없음)' }} · {{ book.bookUid }} · status={{ book.status ?? '-' }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>수량</span>
            <input v-model="createForm.quantity" type="number" min="1" required />
          </label>

          <label class="field">
            <span>externalRef</span>
            <input v-model="createForm.externalRef" type="text" required />
          </label>

          <label class="field">
            <span>externalUserId</span>
            <input v-model="createForm.externalUserId" type="text" required />
          </label>

          <label class="field">
            <span>수령인 이름</span>
            <input v-model="createForm.recipientName" type="text" required />
          </label>

          <label class="field">
            <span>전화번호</span>
            <input v-model="createForm.phoneNumber" type="tel" required placeholder="010-1234-5678" />
          </label>

          <label class="field">
            <span>우편번호</span>
            <input v-model="createForm.postalCode" type="text" required placeholder="12345" />
          </label>

          <label class="field field-wide">
            <span>기본 주소</span>
            <input v-model="createForm.address" type="text" required />
          </label>

          <label class="field field-wide">
            <span>상세 주소</span>
            <input v-model="createForm.addressDetail" type="text" required />
          </label>
        </div>

        <p v-if="booksLoadError" class="error-text">{{ booksLoadError }}</p>
        <p v-if="createValidationError" class="error-text">{{ createValidationError }}</p>
        <p v-if="orderStore.createError" class="error-text">{{ orderStore.createError }}</p>
        <p v-if="createSuccessMessage" class="success-text">{{ createSuccessMessage }}</p>

        <button class="create-button" type="submit" :disabled="orderStore.isCreating || !selectedBookUid">
          {{ orderStore.isCreating ? '생성 중...' : '주문 생성' }}
        </button>
      </form>
    </section>

    <section class="list-section">
      <div class="section-header">
        <h2 class="section-title">주문 목록</h2>
      </div>

      <div v-if="orderStore.isFetchingList" class="loading-state">
        <div class="spinner"></div>
        <p>주문 목록을 불러오는 중입니다...</p>
      </div>

      <div v-else-if="orderStore.listError" class="error-state">
        <p class="error-text">{{ orderStore.listError }}</p>
        <button class="retry-button" type="button" @click="orderStore.fetchOrders(albumId)">다시 시도</button>
      </div>

      <div v-else-if="orderStore.orders.length === 0" class="empty-state">
        <p>아직 생성된 주문이 없습니다.</p>
      </div>

      <div v-else class="order-grid">
        <article v-for="order in orderStore.orders" :key="order.orderId" class="order-card" @click="goToDetail(order.orderId)">
          <div class="card-head">
            <p class="card-date">{{ formatDate(order.createdAt) }}</p>
            <span :class="['status-badge', getStatusClass(order.status)]">{{ getStatusLabel(order.status) }}</span>
          </div>

          <p class="card-id">{{ order.orderUid }}</p>
          <p class="card-meta">{{ order.remoteOrderStatusDisplay }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.order-list-page {
  padding-top: 36px;
  padding-bottom: 72px;
  display: grid;
  gap: 20px;
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

.create-section,
.list-section {
  background: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  border-radius: 16px;
  padding: 20px;
}

.section-header {
  margin-bottom: 12px;
}

.section-title {
  margin: 0;
  font-size: 26px;
}

.section-subtitle {
  margin: 6px 0 0;
  font-size: 14px;
  color: var(--color-olive-gray);
}

.create-form {
  display: grid;
  gap: 12px;
}

.field-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field {
  display: grid;
  gap: 4px;
  font-size: 14px;
  color: var(--color-olive-gray);
}

.field-wide {
  grid-column: 1 / -1;
}

.create-button {
  justify-self: flex-start;
  background: var(--color-terracotta);
  color: var(--color-ivory);
  padding: 10px 16px;
}

.create-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.order-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}

.order-card {
  background: var(--color-white);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 14px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.order-card:hover {
  transform: translateY(-2px);
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.card-date {
  margin: 0;
  font-size: 13px;
  color: var(--color-stone-gray);
}

.card-id {
  margin: 12px 0 4px;
  font-family: var(--font-mono);
  font-size: 14px;
}

.card-meta {
  margin: 0;
  color: var(--color-olive-gray);
  font-size: 14px;
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

.loading-state,
.error-state,
.empty-state {
  text-align: center;
  padding: 28px 10px;
}

.retry-button {
  margin-top: 10px;
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

.error-text {
  margin: 0;
  color: var(--color-error);
  font-size: 14px;
}

.success-text {
  margin: 0;
  color: #2c6d47;
  font-size: 14px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .field-grid {
    grid-template-columns: 1fr;
  }

  .page-title {
    font-size: 30px;
  }
}
</style>

