<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { getAllBooks } from '../../activity/api/activityApi';
import PageHeader from '../../../shared/components/PageHeader.vue';
import OverviewCard from '../../../shared/components/OverviewCard.vue';

const router = useRouter();
const books = ref<any[]>([]);
const isLoading = ref(false);
const errorMessage = ref<string | null>(null);

const loadBooks = async () => {
  isLoading.value = true;
  try {
    books.value = await getAllBooks();
    errorMessage.value = null;
  } catch (err: any) {
    errorMessage.value = err?.message || '책 목록을 불러오지 못했습니다.';
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

onMounted(loadBooks);
</script>

<template>
  <div class="page container">
    <PageHeader title="책 목록" @back="goBack" />

    <OverviewCard title="전체 책" :is-loading="isLoading" @refresh="loadBooks">
      <div v-if="isLoading && books.length === 0" class="state-block">책 목록을 불러오는 중...</div>
      <div v-else-if="errorMessage" class="state-block error">{{ errorMessage }}</div>
      <div v-else-if="books.length === 0" class="state-block">생성된 책이 없습니다.</div>
      <div v-else class="list">
        <div v-for="book in books" :key="book.bookUid" class="list-item">
          <span>{{ book.title || '(제목 없음)' }}</span>
          <span>{{ book.bookUid || '-' }}</span>
          <span>{{ formatDateTime(book.createdAt) }}</span>
        </div>
      </div>
    </OverviewCard>
  </div>
</template>

<style scoped>
.page {
  padding-top: 36px;
  padding-bottom: 72px;
  display: grid;
  gap: 16px;
}

.state-block {
  margin-top: 12px;
  color: var(--color-olive-gray);
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
  grid-template-columns: 2fr 1fr 1fr;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  background: var(--color-white);
}

@media (max-width: 900px) {
  .list-item {
    grid-template-columns: 1fr;
  }
}
</style>
