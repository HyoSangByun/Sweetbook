<script setup lang="ts">
const props = defineProps<{
  canFinalize: boolean;
  isFinalizing: boolean;
  isFinalized: boolean;
}>();

const emit = defineEmits<{
  (e: 'finalize-book'): void;
}>();
</script>

<template>
  <section class="card">
    <h3 class="section-title">5단계: 최종화</h3>
    <button class="btn-primary" :disabled="!canFinalize || isFinalizing" @click="emit('finalize-book')">
      {{ isFinalizing ? '최종화 중...' : '최종화 완료' }}
    </button>
    <p v-if="isFinalized" class="success-message">최종화 완료</p>
  </section>
</template>

