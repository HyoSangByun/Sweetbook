<script setup lang="ts">
defineProps<{
  title: string;
  refreshLabel?: string;
  refreshingLabel?: string;
  isLoading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'refresh'): void;
}>();
</script>

<template>
  <section class="card">
    <div class="card-head">
      <h2>{{ title }}</h2>
      <button type="button" class="refresh-button" :disabled="isLoading" @click="emit('refresh')">
        {{ isLoading ? (refreshingLabel || '새로고침 중...') : (refreshLabel || '새로고침') }}
      </button>
    </div>
    <slot />
  </section>
</template>

<style scoped>
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
</style>
