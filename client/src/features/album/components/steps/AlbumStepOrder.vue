<script setup lang="ts">
const props = defineProps<{
  finalizedBookUidInFlow: string | null;
  finalizedBookTitleInFlow: string;
}>();

const emit = defineEmits<{
  (e: 'go-to-order'): void;
}>();
</script>

<template>
  <section class="card">
    <h3 class="section-title">6단계: 주문 진행</h3>
    <p class="state-message">이번에 최종화된 책 정보입니다.</p>
    <div v-if="finalizedBookUidInFlow" class="finalized-summary">
      <p><strong>{{ finalizedBookTitleInFlow || '(제목 없음)' }}</strong></p>
      <p class="state-message">bookUid: {{ finalizedBookUidInFlow }}</p>
    </div>
    <p v-else class="error-message">이번 생성에서 최종화된 책이 없어 주문으로 이동할 수 없습니다.</p>
    <button class="btn-primary" :disabled="!finalizedBookUidInFlow" @click="emit('go-to-order')">주문하러 가기</button>
  </section>
</template>

