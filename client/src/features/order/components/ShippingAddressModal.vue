<script setup lang="ts">
import { reactive, watch, computed } from 'vue';
import type { ShippingUpdateRequest } from '../types';

const props = defineProps<{
  open: boolean;
  pending: boolean;
  errorMessage: string | null;
}>();

const emit = defineEmits<{
  close: [];
  submit: [payload: ShippingUpdateRequest];
}>();

const form = reactive({
  recipientName: '',
  phoneNumber: '',
  postalCode: '',
  address: '',
  addressDetail: '',
});

const validationError = computed(() => {
  const phonePattern = /^\d{2,3}-?\d{3,4}-?\d{4}$/;
  const postalPattern = /^\d{5}$/;

  if (!form.recipientName.trim()) return '수령인 이름을 입력해 주세요.';
  if (!form.phoneNumber.trim()) return '전화번호를 입력해 주세요.';
  if (!phonePattern.test(form.phoneNumber.trim())) return '전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)';
  if (!form.postalCode.trim()) return '우편번호를 입력해 주세요.';
  if (!postalPattern.test(form.postalCode.trim())) return '우편번호는 5자리 숫자여야 합니다.';
  if (!form.address.trim()) return '기본 주소를 입력해 주세요.';
  if (!form.addressDetail.trim()) return '상세 주소를 입력해 주세요.';

  return null;
});

watch(
  () => props.open,
  (nextOpen) => {
    if (!nextOpen) return;

    // TODO(contract): ORDER_API_CONTRACT.md에는 주문 상세 응답의 shipping 포함 여부가 명시되어 있지 않아 자동 프리필을 하지 않는다.
    form.recipientName = '';
    form.phoneNumber = '';
    form.postalCode = '';
    form.address = '';
    form.addressDetail = '';
  }
);

const handleSubmit = () => {
  if (props.pending || validationError.value) {
    return;
  }

  emit('submit', {
    recipientName: form.recipientName.trim(),
    phoneNumber: form.phoneNumber.trim(),
    postalCode: form.postalCode.trim(),
    address: form.address.trim(),
    addressDetail: form.addressDetail.trim(),
  });
};
</script>

<template>
  <div v-if="open" class="modal-overlay" @click.self="emit('close')">
    <div class="modal-panel" role="dialog" aria-modal="true" aria-labelledby="shipping-modal-title">
      <header class="modal-header">
        <h3 id="shipping-modal-title" class="modal-title">배송지 수정</h3>
        <button class="close-button" type="button" @click="emit('close')" :disabled="pending">닫기</button>
      </header>

      <p class="helper-text">
        현재 API 계약서에는 주문 상세 응답의 기존 배송지 필드가 명시되어 있지 않아 자동 채움 없이 새로 입력합니다.
      </p>

      <form class="shipping-form" @submit.prevent="handleSubmit">
        <label class="form-field">
          <span>수령인 이름</span>
          <input v-model="form.recipientName" type="text" autocomplete="name" required />
        </label>

        <label class="form-field">
          <span>전화번호</span>
          <input v-model="form.phoneNumber" type="tel" autocomplete="tel" required placeholder="010-1234-5678" />
        </label>

        <label class="form-field">
          <span>우편번호</span>
          <input v-model="form.postalCode" type="text" inputmode="numeric" required placeholder="12345" />
        </label>

        <label class="form-field">
          <span>기본 주소</span>
          <input v-model="form.address" type="text" autocomplete="street-address" required />
        </label>

        <label class="form-field">
          <span>상세 주소</span>
          <input v-model="form.addressDetail" type="text" required />
        </label>

        <p v-if="validationError" class="error-text">{{ validationError }}</p>
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

        <div class="actions">
          <button type="button" class="secondary-button" @click="emit('close')" :disabled="pending">취소</button>
          <button type="submit" class="primary-button" :disabled="pending || !!validationError">
            {{ pending ? '저장 중...' : '저장' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
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

.helper-text {
  margin: 0 0 16px;
  color: var(--color-olive-gray);
  font-size: 14px;
}

.shipping-form {
  display: grid;
  gap: 12px;
}

.form-field {
  display: grid;
  gap: 6px;
  font-size: 14px;
  color: var(--color-olive-gray);
}

.actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 8px;
}

.secondary-button {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 10px 16px;
}

.primary-button {
  background: var(--color-terracotta);
  color: var(--color-ivory);
  padding: 10px 16px;
}

.primary-button:disabled,
.secondary-button:disabled,
.close-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.error-text {
  margin: 0;
  color: var(--color-error);
  font-size: 14px;
}

@media (max-width: 640px) {
  .modal-panel {
    padding: 20px;
  }

  .actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
}
</style>

