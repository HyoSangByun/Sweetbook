<script setup lang="ts">
const props = defineProps<{
  canAddContents: boolean;
  isAddingContents: boolean;
  isContentsAdded: boolean;
}>();

const emit = defineEmits<{
  (e: 'add-contents'): void;
}>();
</script>

<template>
  <section class="card">
    <h3 class="section-title">4단계: 내지 추가</h3>
    <p class="state-message">고정 템플릿 UID: <strong>3T09l6GEd0AL</strong></p>
    <p class="state-message">사진을 선택하지 않으면 기본 이미지(https://placehold.co/300x200.jpg)가 자동 적용됩니다.</p>
    <button class="btn-primary" :disabled="!canAddContents || isAddingContents" @click="emit('add-contents')">
      {{ isAddingContents ? '추가 중...' : '내지 추가' }}
    </button>
    <p v-if="isContentsAdded" class="success-message">내지 추가 완료</p>
  </section>
</template>

