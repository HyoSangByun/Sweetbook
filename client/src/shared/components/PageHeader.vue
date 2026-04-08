<script setup lang="ts">
defineProps<{
  title: string;
  backLabel?: string;
}>();

const emit = defineEmits<{
  (e: 'back'): void;
}>();
</script>

<template>
  <header class="page-header">
    <button class="back-button" type="button" @click="emit('back')">{{ backLabel || '홈으로' }}</button>
    <h1 class="page-title">{{ title }}</h1>
  </header>
</template>

<style scoped>
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
</style>
