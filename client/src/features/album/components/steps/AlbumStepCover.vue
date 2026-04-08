<script setup lang="ts">
const props = defineProps<{
  bookUid: string | null;
  canApplyCover: boolean;
  isApplyingCover: boolean;
  isCoverApplied: boolean;
  coverSubtitle: string;
}>();

const emit = defineEmits<{
  (e: 'update:coverSubtitle', value: string): void;
  (e: 'apply-cover'): void;
}>();
</script>

<template>
  <section class="card">
    <h3 class="section-title">3단계: 표지 적용</h3>
    <p class="state-message">고정 템플릿 UID: <strong>4Fy1mpIlm1ek</strong></p>
    <label class="field">
      <span>표지 부제목 (기본값: 책 제목)</span>
      <input
        :value="coverSubtitle"
        type="text"
        maxlength="255"
        :disabled="!bookUid"
        @input="emit('update:coverSubtitle', ($event.target as HTMLInputElement).value)"
      />
    </label>
    <button class="btn-primary" :disabled="!canApplyCover || isApplyingCover" @click="emit('apply-cover')">
      {{ isApplyingCover ? '적용 중...' : '표지 추가' }}
    </button>
    <p v-if="isCoverApplied" class="success-message">표지 적용 완료</p>
  </section>
</template>

