<template>
  <div class="album-detail-page">
    <header class="page-header">
      <div class="container header-content">
        <router-link to="/" class="btn-back">활동 목록으로</router-link>
        <h1 class="logo">SweetBook</h1>
        <button @click="handleLogout" class="btn-logout">로그아웃</button>
      </div>
    </header>

    <main class="container" v-if="albumStore.currentAlbum && !fetchError">
      <section class="album-info-section">
        <div class="month-badge">{{ albumStore.currentAlbum.month }}</div>
        <h2 class="album-title">{{ albumStore.currentAlbum.title }}</h2>
      </section>

      <section class="book-config card">
        <h3 class="section-title">책 생성 정보</h3>
        <div class="field-grid">
          <label class="field">
            <span>Book title</span>
            <input v-model="bookForm.title" type="text" required />
          </label>

          <label class="field">
            <span>BookSpec (판형)</span>
            <select v-model="bookForm.bookSpecUid">
              <option value="" disabled>선택하세요</option>
              <option v-for="spec in bookSpecs" :key="spec.bookSpecUid" :value="spec.bookSpecUid">
                {{ spec.bookSpecUid }} ({{ spec.pageMin }}~{{ spec.pageMax }})
              </option>
            </select>
          </label>

          <label class="field">
            <span>Cover template</span>
            <select v-model="bookForm.coverTemplateUid">
              <option value="" disabled>선택하세요</option>
              <option v-for="tpl in coverTemplates" :key="tpl.templateUid" :value="tpl.templateUid">
                {{ tpl.templateUid }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Content template</span>
            <select v-model="bookForm.contentTemplateUid">
              <option value="" disabled>선택하세요</option>
              <option v-for="tpl in contentTemplates" :key="tpl.templateUid" :value="tpl.templateUid">
                {{ tpl.templateUid }}
              </option>
            </select>
          </label>
        </div>
      </section>

      <section class="activities-section card">
        <h3 class="section-title">선택된 활동/날짜 ({{ albumStore.currentAlbum.selectedActivityCount }})</h3>
        <div v-if="albumStore.currentAlbum.selectedActivities.length === 0" class="empty-state">
          선택된 활동이 없습니다.
        </div>
        <div v-else class="activity-list">
          <div v-for="activity in albumStore.currentAlbum.selectedActivities" :key="activity.albumActivityId" class="activity-item">
            <div class="activity-meta">
              <strong>{{ formatDate(activity.activityDateTime) }}</strong>
              <span>{{ activity.activityName }}</span>
              <span>{{ activity.distanceKm.toFixed(2) }}km</span>
            </div>
            <div class="photo-section">
              <label class="file-label">
                사진 첨부(선택)
                <input type="file" accept="image/*" @change="onPhotoChange(activity.activityId, $event)" />
              </label>
              <div v-if="photoPreviewByActivity[activity.activityId]" class="preview-box">
                <img :src="photoPreviewByActivity[activity.activityId]" alt="첨부 미리보기" class="preview-image" />
                <button type="button" class="btn-remove" @click="clearPhoto(activity.activityId)">제거</button>
              </div>
            </div>
            <button class="btn-deselect" @click="handleDeselect(activity.activityId)">활동 해제</button>
          </div>
        </div>
      </section>

      <section class="preview-section card">
        <div class="preview-actions">
          <button class="btn-primary" @click="openPreview" :disabled="isPreviewLoading || isGenerating">
            {{ isPreviewLoading ? '계산 중...' : '미리보기' }}
          </button>
        </div>
        <p v-if="previewError" class="error-message">{{ previewError }}</p>
        <p v-if="generateError" class="error-message">{{ generateError }}</p>

        <div v-if="showPreview" class="preview-summary">
          <h4>미리보기 요약</h4>
          <p>제목: {{ bookForm.title }}</p>
          <p>BookSpec: {{ selectedBookSpecLabel }}</p>
          <p>예상 페이지 수: {{ estimate?.estimatedPageCount ?? estimatedPageCount }}</p>
          <p v-if="estimate?.totalAmount != null">
            예상 금액: {{ formatCurrency(estimate.totalAmount, estimate.currency || 'KRW') }}
          </p>
          <div class="thumb-row">
            <div class="thumb-col">
              <p>Cover</p>
              <img v-if="coverThumbnail" :src="coverThumbnail" alt="cover thumbnail" class="thumb" />
            </div>
            <div class="thumb-col">
              <p>Content</p>
              <img v-if="contentThumbnail" :src="contentThumbnail" alt="content thumbnail" class="thumb" />
            </div>
          </div>
          <button class="btn-primary" @click="confirmGenerateAndGoOrder" :disabled="isGenerating">
            {{ isGenerating ? '생성 중...' : '확인 후 결제 진행' }}
          </button>
        </div>
      </section>
    </main>

    <div v-else-if="fetchError" class="error-state">
      <p class="error-message">{{ fetchError }}</p>
      <button @click="loadAlbumData" class="btn-primary">다시 시도</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../auth/store';
import { useAlbumStore } from '../store';
import * as albumApi from '../api/albumApi';

type BookSpecItem = {
  bookSpecUid: string;
  pageMin?: number;
  pageMax?: number;
};

type TemplateItem = {
  templateUid: string;
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const albumStore = useAlbumStore();

const fetchError = ref<string | null>(null);
const previewError = ref<string | null>(null);
const generateError = ref<string | null>(null);
const isPreviewLoading = ref(false);
const isGenerating = ref(false);
const showPreview = ref(false);
const estimate = ref<{
  estimatedPageCount: number;
  productAmount: number | null;
  shippingFee: number | null;
  packagingFee: number | null;
  totalAmount: number | null;
  currency: string;
} | null>(null);

const bookSpecs = ref<BookSpecItem[]>([]);
const coverTemplates = ref<TemplateItem[]>([]);
const contentTemplates = ref<TemplateItem[]>([]);
const coverThumbnail = ref<string | null>(null);
const contentThumbnail = ref<string | null>(null);

const bookForm = reactive({
  title: '',
  bookSpecUid: '',
  coverTemplateUid: '',
  contentTemplateUid: '',
});

const photoFileByActivity = ref<Record<number, File | null>>({});
const photoPreviewByActivity = ref<Record<number, string>>({});

const estimatedPageCount = computed(() => (albumStore.currentAlbum?.selectedActivities.length ?? 0) + 1);

const selectedBookSpecLabel = computed(() => {
  const spec = bookSpecs.value.find((item) => item.bookSpecUid === bookForm.bookSpecUid);
  return spec ? spec.bookSpecUid : '-';
});

const loadAlbumData = async () => {
  const albumId = Number(route.params.id);
  if (!albumId) return;

  fetchError.value = null;
  try {
    await albumStore.fetchAlbum(albumId);
    if (albumStore.currentAlbum && !bookForm.title) {
      bookForm.title = albumStore.currentAlbum.title;
    }
  } catch (err: any) {
    fetchError.value = err.message || '앨범 정보를 불러오지 못했습니다.';
  }
};

const loadBookSpecs = async () => {
  const specs = await albumApi.getBookSpecs();
  bookSpecs.value = specs as BookSpecItem[];
  if (!bookForm.bookSpecUid && specs.length > 0) {
    bookForm.bookSpecUid = (specs[0] as BookSpecItem).bookSpecUid;
  }
};

const loadTemplates = async () => {
  if (!bookForm.bookSpecUid) return;
  const [covers, contents] = await Promise.all([
    albumApi.getTemplates(bookForm.bookSpecUid, 'cover'),
    albumApi.getTemplates(bookForm.bookSpecUid, 'content'),
  ]);
  coverTemplates.value = covers as TemplateItem[];
  contentTemplates.value = contents as TemplateItem[];

  if (!bookForm.coverTemplateUid && coverTemplates.value.length > 0) {
    bookForm.coverTemplateUid = coverTemplates.value[0].templateUid;
  }
  if (!bookForm.contentTemplateUid && contentTemplates.value.length > 0) {
    bookForm.contentTemplateUid = contentTemplates.value[0].templateUid;
  }
};

const loadTemplateThumbnail = async (templateUid: string, type: 'cover' | 'content') => {
  if (!templateUid) {
    if (type === 'cover') coverThumbnail.value = null;
    else contentThumbnail.value = null;
    return;
  }

  const detail = await albumApi.getTemplateDetail(templateUid);
  const thumbnail = detail?.thumbnails?.layout ?? null;
  if (type === 'cover') {
    coverThumbnail.value = thumbnail;
  } else {
    contentThumbnail.value = thumbnail;
  }
};

const onPhotoChange = (activityId: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;

  const prevUrl = photoPreviewByActivity.value[activityId];
  if (prevUrl) {
    URL.revokeObjectURL(prevUrl);
  }

  photoFileByActivity.value[activityId] = file;
  photoPreviewByActivity.value[activityId] = URL.createObjectURL(file);
};

const clearPhoto = (activityId: number) => {
  const prevUrl = photoPreviewByActivity.value[activityId];
  if (prevUrl) {
    URL.revokeObjectURL(prevUrl);
  }
  delete photoPreviewByActivity.value[activityId];
  photoFileByActivity.value[activityId] = null;
};

const revokeAllPreviews = () => {
  Object.values(photoPreviewByActivity.value).forEach((url) => URL.revokeObjectURL(url));
  photoPreviewByActivity.value = {};
};

const handleDeselect = async (activityId: number) => {
  if (!albumStore.currentAlbum) return;
  await albumStore.deselectActivity(albumStore.currentAlbum.albumId, activityId);
  if (albumStore.currentAlbum) {
    albumStore.currentAlbum.selectedActivities = albumStore.currentAlbum.selectedActivities.filter(
      (activity) => activity.activityId !== activityId,
    );
  }
  clearPhoto(activityId);
};

const validateBeforePreview = () => {
  if (!albumStore.currentAlbum || albumStore.currentAlbum.selectedActivities.length === 0) {
    return '활동을 최소 1개 이상 선택해 주세요.';
  }
  if (!bookForm.title.trim()) return '책 제목을 입력해 주세요.';
  if (!bookForm.bookSpecUid) return 'BookSpec을 선택해 주세요.';
  if (!bookForm.coverTemplateUid) return 'Cover template을 선택해 주세요.';
  if (!bookForm.contentTemplateUid) return 'Content template을 선택해 주세요.';
  return null;
};

const openPreview = async () => {
  previewError.value = validateBeforePreview();
  if (previewError.value || !albumStore.currentAlbum) return;

  isPreviewLoading.value = true;
  try {
    estimate.value = await albumApi.estimateBookOrder(albumStore.currentAlbum.albumId, {
      title: bookForm.title.trim(),
      bookSpecUid: bookForm.bookSpecUid,
      coverTemplateUid: bookForm.coverTemplateUid,
      contentTemplateUid: bookForm.contentTemplateUid,
    });
    showPreview.value = true;
  } catch (err: any) {
    previewError.value = err?.message || '미리보기 견적 계산에 실패했습니다.';
  } finally {
    isPreviewLoading.value = false;
  }
};

const confirmGenerateAndGoOrder = async () => {
  if (!albumStore.currentAlbum || isGenerating.value) return;

  isGenerating.value = true;
  generateError.value = null;

  try {
    await albumStore.updateAlbum(albumStore.currentAlbum.albumId, { title: bookForm.title.trim() });
    await albumStore.generateBook(albumStore.currentAlbum.albumId);
    router.push({ name: 'order-list', params: { albumId: albumStore.currentAlbum.albumId } });
  } catch (err: any) {
    generateError.value = err?.message || '책 생성 중 오류가 발생했습니다. 다시 시도해 주세요.';
  } finally {
    isGenerating.value = false;
  }
};

const handleLogout = () => {
  authStore.logout();
  router.push({ name: 'login' });
};

const formatDate = (value: string) => {
  return new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
};

const formatCurrency = (amount: number, currency: string) => {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency }).format(amount);
};

watch(
  () => bookForm.bookSpecUid,
  async () => {
    showPreview.value = false;
    estimate.value = null;
    generateError.value = null;
    bookForm.coverTemplateUid = '';
    bookForm.contentTemplateUid = '';
    await loadTemplates();
  },
);

watch(
  () => bookForm.coverTemplateUid,
  async (value) => {
    showPreview.value = false;
    estimate.value = null;
    generateError.value = null;
    await loadTemplateThumbnail(value, 'cover');
  },
);

watch(
  () => bookForm.contentTemplateUid,
  async (value) => {
    showPreview.value = false;
    estimate.value = null;
    generateError.value = null;
    await loadTemplateThumbnail(value, 'content');
  },
);

onMounted(async () => {
  await loadAlbumData();
  await loadBookSpecs();
  await loadTemplates();
  await loadTemplateThumbnail(bookForm.coverTemplateUid, 'cover');
  await loadTemplateThumbnail(bookForm.contentTemplateUid, 'content');
});

onBeforeUnmount(() => {
  revokeAllPreviews();
});
</script>

<style scoped>
.album-detail-page {
  min-height: 100vh;
  background-color: var(--color-parchment);
}

.page-header {
  background-color: var(--color-white);
  border-bottom: 1px solid var(--color-border-cream);
  padding: 16px 0;
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-back {
  font-size: 0.875rem;
  color: var(--color-olive-gray);
  text-decoration: none;
}

.logo {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin: 0;
}

.btn-logout {
  background: none;
  color: var(--color-terracotta);
  font-size: 0.875rem;
  padding: 4px 8px;
  font-weight: 500;
}

.card {
  background: var(--color-white);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.album-info-section {
  margin-bottom: 16px;
}

.month-badge {
  display: inline-block;
  padding: 4px 12px;
  background-color: var(--color-warm-sand);
  border-radius: 20px;
  font-size: 0.875rem;
  color: var(--color-olive-gray);
}

.album-title {
  margin: 8px 0 0;
}

.section-title {
  margin: 0 0 12px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field {
  display: grid;
  gap: 6px;
}

.activity-list {
  display: grid;
  gap: 12px;
}

.activity-item {
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  padding: 12px;
  display: grid;
  gap: 10px;
}

.activity-meta {
  display: grid;
  gap: 4px;
}

.photo-section {
  display: grid;
  gap: 8px;
}

.file-label {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.preview-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-image {
  width: 96px;
  height: 96px;
  object-fit: cover;
  border-radius: 8px;
}

.btn-remove,
.btn-deselect {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 6px 10px;
}

.preview-actions {
  display: flex;
  justify-content: flex-start;
}

.btn-primary {
  background: var(--color-terracotta);
  color: var(--color-white);
  padding: 10px 14px;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.thumb-row {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.thumb-col {
  display: grid;
  gap: 8px;
}

.thumb {
  width: 100%;
  max-width: 280px;
  border-radius: 10px;
}

.error-message {
  color: var(--color-error);
}

@media (max-width: 768px) {
  .field-grid,
  .thumb-row {
    grid-template-columns: 1fr;
  }
}
</style>
