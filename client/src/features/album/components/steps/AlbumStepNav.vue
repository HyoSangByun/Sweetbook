<script setup lang="ts">
const props = defineProps<{
  currentStep: number;
  maxNavigableStep: number;
}>();

const emit = defineEmits<{
  (e: 'prev'): void;
  (e: 'next'): void;
}>();
</script>

<template>
  <section class="card step-nav">
    <button type="button" class="btn-secondary" :disabled="currentStep <= 1" @click="emit('prev')">이전 단계</button>
    <button type="button" class="btn-secondary" :disabled="currentStep >= maxNavigableStep" @click="emit('next')">다음 단계</button>
  </section>
</template>

