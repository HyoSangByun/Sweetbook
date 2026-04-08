<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { cancelOrderByUid, getAllOrders, updateOrderShippingByUid } from '../../activity/api/activityApi';

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
  address1: '',
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

const resolveOrderUid = (order: OrderItem) => {
  return String(order.orderUid || '');
};

const resolveStatus = (order: OrderItem) => {
  return String(order.statusDisplay || order.orderStatusDisplay || order.status || '-');
};

const resolveShipping = (order: OrderItem) => {
  const shipping = (order.shipping ?? order.shippingAddress ?? {}) as Record<string, unknown>;
  return {
    recipientName: String(shipping.recipientName ?? shipping.name ?? ''),
    address1: String(shipping.address1 ?? shipping.address ?? ''),
  };
};

const handleCancelOrder = async (order: OrderItem) => {
  const orderUid = resolveOrderUid(order);
  if (!orderUid) {
    actionMessage.value = 'orderUid가 없어 취소할 수 없습니다.';
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
    actionMessage.value = `주문이 취소되었습니다. (${orderUid})`;
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
  shippingForm.address1 = shipping.address1;
  editingOrderUid.value = orderUid;
  actionMessage.value = null;
};

const closeShippingEditor = () => {
  editingOrderUid.value = null;
  shippingForm.recipientName = '';
  shippingForm.address1 = '';
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
    await updateOrderShippingByUid(
      editingOrderUid.value,
      shippingForm.recipientName.trim(),
      shippingForm.address1.trim()
    );
    actionMessage.value = `배송지가 수정되었습니다. (${editingOrderUid.value})`;
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
    <header class="page-header">
      <button class="back-button" type="button" @click="goBack">홈으로</button>
      <h1 class="page-title">주문 목록</h1>
    </header>

    <section class="card">
      <div class="card-head">
        <h2>전체 주문</h2>
        <button type="button" class="refresh-button" :disabled="isLoading" @click="loadOrders">
          {{ isLoading ? '새로고침 중...' : '새로고침' }}
        </button>
      </div>

      <p v-if="actionMessage" class="state-block action">{{ actionMessage }}</p>
      <div v-if="isLoading && orders.length === 0" class="state-block">주문 목록을 불러오는 중...</div>
      <div v-else-if="errorMessage" class="state-block error">{{ errorMessage }}</div>
      <div v-else-if="orders.length === 0" class="state-block">주문 내역이 없습니다.</div>
      <div v-else class="list">
        <article v-for="order in orders" :key="resolveOrderUid(order)" class="list-item">
          <div class="item-head">
            <strong>{{ resolveOrderUid(order) || '-' }}</strong>
            <span>{{ resolveStatus(order) }}</span>
          </div>
          <p class="meta">생성일: {{ formatDateTime(order.createdAt) }}</p>
          <div class="actions">
            <button
              type="button"
              class="cancel-button"
              :disabled="isCancellingUid === resolveOrderUid(order)"
              @click="handleCancelOrder(order)"
            >
              {{ isCancellingUid === resolveOrderUid(order) ? '취소 중...' : '주문 취소' }}
            </button>
            <button type="button" class="shipping-button" @click="openShippingEditor(order)">
              배송지 변경
            </button>
          </div>
        </article>
      </div>
    </section>

    <section v-if="editingOrderUid" class="card">
      <div class="card-head">
        <h2>배송지 변경</h2>
        <button type="button" class="refresh-button" @click="closeShippingEditor">닫기</button>
      </div>
      <p class="meta">주문 UID: {{ editingOrderUid }}</p>
      <div class="form-grid">
        <label class="field">
          <span>수령인 이름</span>
          <input v-model="shippingForm.recipientName" type="text" maxlength="100" />
        </label>
        <label class="field">
          <span>기본 주소</span>
          <input v-model="shippingForm.address1" type="text" maxlength="200" />
        </label>
      </div>
      <div class="actions">
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
    </section>
  </div>
</template>

<style scoped>
.page {
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
.shipping-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.form-grid {
  display: grid;
  gap: 10px;
  margin-top: 8px;
}
.field {
  display: grid;
  gap: 6px;
}
</style>
