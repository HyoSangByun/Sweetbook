<script setup lang="ts">
const props = defineProps<{
  canCreateDraft: boolean;
  isCreatingDraft: boolean;
  minActivityCount: number;
  bookTitle: string;
}>();

const emit = defineEmits<{
  (e: 'update:bookTitle', value: string): void;
  (e: 'create-draft'): void;
}>();
</script>

<template>
  <section class="card">
    <h3 class="section-title">1단계: 책 Draft 생성</h3>
    <label class="field">
      <span>책 제목</span>
      <input :value="bookTitle" type="text" maxlength="255" @input="emit('update:bookTitle', ($event.target as HTMLInputElement).value)" />
    </label>
    <p class="state-message">활동 최소 선택 수: {{ minActivityCount }}개</p>
    <p class="state-message">고정 판형: <strong>SQUAREBOOK_HC</strong></p>
    <button class="btn-primary" :disabled="isCreatingDraft || !canCreateDraft" @click="emit('create-draft')">
      {{ isCreatingDraft ? '생성 중...' : '책 Draft 생성' }}
    </button>
  </section>
</template>

