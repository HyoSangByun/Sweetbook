<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { cancelOrderByUid, getAllOrders, updateOrderShippingByUid } from '../../activity/api/activityApi';
import { openKakaoPostcode } from '../../../shared/utils/kakaoPostcode';
import PageHeader from '../../../shared/components/PageHeader.vue';
import OverviewCard from '../../../shared/components/OverviewCard.vue';

type OrderItem = Record<string, any>;

const router = useRouter();
const orders = ref<OrderItem[]>([]);
const isLoading = ref(false);
const errorMessage = ref<string | null>(null);
const actionMessage = ref<string | null>(null);
const isCancellingUid = ref<string | null>(null);
const isUpdatingUid = ref<string | null>(null);
const editingOrderUid = ref<string | null>(null);

const shippingForm = reactive({
  recipientName: '',
  postalCode: '',
  address1: '',
  address2: '',
});

const shippingFormError = computed(() => {
  if (!shippingForm.recipientName.trim()) return '수령인 이름을 입력해 주세요.';
  if (!shippingForm.address1.trim()) return '기본 주소를 입력해 주세요.';
  return null;
});

const loadOrders = async () => {
  isLoading.value = true;
  try {
    orders.value = await getAllOrders();
    errorMessage.value = null;
  } catch (err: any) {
    errorMessage.value = err?.message || '주문 목록을 불러오지 못했습니다.';
  } finally {
    isLoading.value = false;
  }
};

const goBack = () => {
  router.push({ name: 'dashboard' });
};

const formatDateTime = (value?: string) => {
  if (!value) return '-';
  return new Date(value).toLocaleString('ko-KR');
};

const resolveOrderUid = (order: OrderItem) => String(order.orderUid || '');

const resolveStatusText = (order: OrderItem) => String(order.statusDisplay || order.orderStatusDisplay || order.status || '-');

const resolveStatusCode = (order: OrderItem) => {
  const raw = order.orderStatus ?? order.statusCode ?? order.remoteOrderStatusCode ?? null;
  if (raw === null || raw === undefined || raw === '') return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
};

const isCancelable = (order: OrderItem) => {
  const code = resolveStatusCode(order);
  return code === 20 || code === 25;
};

const statusBadgeClass = (order: OrderItem) => {
  const text = resolveStatusText(order).toUpperCase();
  const code = resolveStatusCode(order);
  if (code === 20 || text.includes('PAID') || text.includes('결제완료')) return 'status-paid';
  if (code === 81 || text.includes('CANCELLED_REFUND') || text.includes('취소환불')) return 'status-cancel-refund';
  return 'status-default';
};

const pickFirstString = (...candidates: unknown[]) => {
  for (const value of candidates) {
    if (typeof value === 'string' && value.trim().length > 0) return value;
  }
  return '';
};

const resolveShipping = (order: OrderItem) => {
  const shipping = (order.shipping ?? order.shippingAddress ?? order.payload?.shipping ?? {}) as Record<string, unknown>;
  return {
    recipientName: pickFirstString(
      shipping.recipientName,
      shipping.name,
      order.recipientName,
      order.shippingRecipientName
    ),
    postalCode: pickFirstString(
      shipping.postalCode,
      shipping.zipCode,
      order.postalCode,
      order.zipCode
    ),
    address1: pickFirstString(
      shipping.address1,
      shipping.address,
      order.address1,
      order.shippingAddress1
    ),
    address2: pickFirstString(
      shipping.address2,
      order.address2,
      order.shippingAddress2
    ),
  };
};

const handleCancelOrder = async (order: OrderItem) => {
  const orderUid = resolveOrderUid(order);
  if (!orderUid) {
    actionMessage.value = 'orderUid가 없어 취소할 수 없습니다.';
    return;
  }
  if (!isCancelable(order)) {
    actionMessage.value = '이미 취소된 주문이거나 취소 가능한 상태가 아닙니다.';
    return;
  }

  const cancelReason = window.prompt('취소 사유를 입력해 주세요.', '사용자 요청');
  if (cancelReason === null) return;
  if (!cancelReason.trim()) {
    actionMessage.value = '취소 사유(cancelReason)는 필수입니다.';
    return;
  }

  actionMessage.value = null;
  isCancellingUid.value = orderUid;
  try {
    await cancelOrderByUid(orderUid, cancelReason.trim());
    actionMessage.value = `주문을 취소했습니다. (${orderUid})`;
    await loadOrders();
  } catch (err: any) {
    actionMessage.value = err?.message || '주문 취소에 실패했습니다.';
  } finally {
    isCancellingUid.value = null;
  }
};

const openShippingEditor = (order: OrderItem) => {
  const orderUid = resolveOrderUid(order);
  if (!orderUid) {
    actionMessage.value = 'orderUid가 없어 배송지를 수정할 수 없습니다.';
    return;
  }

  const shipping = resolveShipping(order);
  shippingForm.recipientName = shipping.recipientName;
  shippingForm.postalCode = shipping.postalCode;
  shippingForm.address1 = shipping.address1;
  shippingForm.address2 = shipping.address2;

  editingOrderUid.value = orderUid;
  actionMessage.value = null;
};

const closeShippingEditor = () => {
  editingOrderUid.value = null;
  shippingForm.recipientName = '';
  shippingForm.postalCode = '';
  shippingForm.address1 = '';
  shippingForm.address2 = '';
};

const handleSearchAddress = async () => {
  try {
    const selected = await openKakaoPostcode();
    shippingForm.postalCode = selected.zonecode;
    shippingForm.address1 = selected.address;
  } catch (error: any) {
    window.alert(error?.message || '주소 검색을 열지 못했습니다.');
  }
};

const submitShippingUpdate = async () => {
  if (!editingOrderUid.value) return;
  if (shippingFormError.value) {
    actionMessage.value = shippingFormError.value;
    return;
  }

  actionMessage.value = null;
  isUpdatingUid.value = editingOrderUid.value;
  try {
    await updateOrderShippingByUid(editingOrderUid.value, {
      recipientName: shippingForm.recipientName.trim(),
      postalCode: shippingForm.postalCode.trim() || undefined,
      address1: shippingForm.address1.trim(),
      address2: shippingForm.address2.trim() || undefined,
    });
    actionMessage.value = `배송지 수정이 완료되었습니다. (${editingOrderUid.value})`;
    closeShippingEditor();
    await loadOrders();
  } catch (err: any) {
    actionMessage.value = err?.message || '배송지 수정에 실패했습니다.';
  } finally {
    isUpdatingUid.value = null;
  }
};

onMounted(loadOrders);
</script>

<template>
  <div class="page container">
    <PageHeader title="주문 목록" @back="goBack" />

    <OverviewCard title="전체 주문" :is-loading="isLoading" @refresh="loadOrders">
      <p v-if="actionMessage" class="state-block action">{{ actionMessage }}</p>
      <div v-if="isLoading && orders.length === 0" class="state-block">주문 목록을 불러오는 중입니다...</div>
      <div v-else-if="errorMessage" class="state-block error">{{ errorMessage }}</div>
      <div v-else-if="orders.length === 0" class="state-block">주문 내역이 없습니다.</div>
      <div v-else class="list">
        <article v-for="order in orders" :key="resolveOrderUid(order)" class="list-item">
          <div class="item-head">
            <strong>{{ resolveOrderUid(order) || '-' }}</strong>
            <span class="status-badge" :class="statusBadgeClass(order)">
              {{ resolveStatusText(order) }}
            </span>
          </div>
          <p class="meta">생성일: {{ formatDateTime(order.createdAt) }}</p>
          <div class="actions">
            <button
              type="button"
              class="cancel-button"
              :disabled="!isCancelable(order) || isCancellingUid === resolveOrderUid(order)"
              @click="handleCancelOrder(order)"
            >
              {{ isCancellingUid === resolveOrderUid(order) ? '취소 중...' : '주문 취소' }}
            </button>
            <button type="button" class="shipping-button" @click="openShippingEditor(order)">배송지 변경</button>
          </div>
        </article>
      </div>
    </OverviewCard>

    <Teleport to="body">
      <div v-if="editingOrderUid" class="modal-overlay" @click.self="closeShippingEditor">
        <div class="modal-panel">
        <div class="modal-header">
          <h3 class="modal-title">배송지 변경</h3>
          <button type="button" class="close-button" @click="closeShippingEditor" :disabled="isUpdatingUid === editingOrderUid">
            닫기
          </button>
        </div>

        <p class="meta">주문 UID: {{ editingOrderUid }}</p>

        <div class="form-grid">
          <label class="field">
            <span>수령인 이름</span>
            <input v-model="shippingForm.recipientName" type="text" maxlength="100" />
          </label>
          <label class="field">
            <span>우편번호</span>
            <div class="inline-field">
              <input v-model="shippingForm.postalCode" type="text" maxlength="10" placeholder="12345" />
              <button type="button" class="search-address-button" @click="handleSearchAddress">우편번호 검색</button>
            </div>
          </label>
          <label class="field">
            <span>기본 주소</span>
            <input v-model="shippingForm.address1" type="text" maxlength="200" />
          </label>
          <label class="field">
            <span>상세 주소</span>
            <input v-model="shippingForm.address2" type="text" maxlength="200" />
          </label>
        </div>

        <div class="modal-actions">
          <button type="button" class="close-button" @click="closeShippingEditor" :disabled="isUpdatingUid === editingOrderUid">
            취소
          </button>
          <button
            type="button"
            class="shipping-button"
            :disabled="!!shippingFormError || isUpdatingUid === editingOrderUid"
            @click="submitShippingUpdate"
          >
            {{ isUpdatingUid === editingOrderUid ? '수정 중...' : '배송지 저장' }}
          </button>
        </div>

        <p v-if="shippingFormError" class="state-block error">{{ shippingFormError }}</p>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page {
  padding-top: 36px;
  padding-bottom: 72px;
  display: grid;
  gap: 16px;
}

.state-block {
  margin-top: 12px;
  color: var(--color-olive-gray);
}

.action {
  color: #2c6d47;
}

.error {
  color: var(--color-error);
}

.list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.list-item {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  background: var(--color-white);
}

.item-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-paid {
  background: #e6f6ea;
  color: #1f7a3f;
}

.status-cancel-refund {
  background: #fde7e7;
  color: #b23939;
}

.status-default {
  background: #ece9e1;
  color: #5f584d;
}

.meta {
  margin: 0;
  color: var(--color-olive-gray);
  font-size: 14px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.cancel-button {
  background: #fff;
  color: var(--color-error);
  border: 1px solid var(--color-error);
  padding: 8px 12px;
}

.shipping-button {
  background: var(--color-terracotta);
  color: var(--color-white);
  padding: 8px 12px;
}

.cancel-button:disabled,
.shipping-button:disabled,
.search-address-button:disabled,
.close-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(20, 20, 19, 0.45);
  display: grid;
  place-items: center;
  padding: 24px;
  z-index: 1000;
}

.modal-panel {
  width: min(560px, 100%);
  border-radius: 16px;
  background: var(--color-ivory);
  border: 1px solid var(--color-border-cream);
  box-shadow: 0 0 0 1px var(--color-ring-warm), rgba(0, 0, 0, 0.08) 0 12px 28px;
  padding: 24px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.modal-title {
  margin: 0;
  font-size: 28px;
}

.close-button {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
}

.form-grid {
  display: grid;
  gap: 16px;
  margin-top: 14px;
}

.field {
  display: grid;
  gap: 8px;
}

.inline-field {
  display: flex;
  align-items: center;
  gap: 14px;
}

.inline-field input {
  flex: 1;
}

.search-address-button {
  white-space: nowrap;
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 14px;
  margin-top: 22px;
}
</style>
