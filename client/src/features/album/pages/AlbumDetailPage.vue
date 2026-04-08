<template>
  <div class="album-detail-page">
    <header class="page-header">
      <div class="container header-content">
        <router-link to="/" class="btn-back">활동 목록으로</router-link>
        <h1 class="logo">SweetBook</h1>
      </div>
    </header>

    <main class="container" v-if="albumStore.currentAlbum && !fetchError">
      <section class="card">
        <h2 class="album-title">{{ albumStore.currentAlbum.title }}</h2>
        <p class="state-message">{{ albumStore.currentAlbum.month }}</p>
      </section>

      <AlbumStepHeader
        :current-step="currentStep"
        :max-navigable-step="maxNavigableStep"
        :step-items="STEP_ITEMS"
        @go-to-step="goToStep"
      />

      <AlbumStepDraft
        v-if="currentStep === 1"
        :book-title="bookTitle"
        :can-create-draft="canCreateDraft"
        :is-creating-draft="isCreatingDraft"
        :min-activity-count="MIN_ACTIVITY_COUNT"
        @update:book-title="bookTitle = $event"
        @create-draft="createDraftBook"
      />

      <AlbumStepPhotos
        v-if="currentStep === 2"
        :book-uid="bookUid"
        :is-uploading-photo="isUploadingPhoto"
        :cover-front-preview-url="coverFrontPreviewUrl"
        :cover-front-file-name="coverFrontFileName"
        :selected-activities="albumStore.currentAlbum.selectedActivities"
        :activity-photos="activityPhotos"
        @cover-file-change="onCoverFileChange"
        @activity-files-change="onActivityFilesChange"
      />

      <AlbumStepCover
        v-if="currentStep === 3"
        :book-uid="bookUid"
        :cover-subtitle="coverSubtitle"
        :can-apply-cover="canApplyCover"
        :is-applying-cover="isApplyingCover"
        :is-cover-applied="isCoverApplied"
        @update:cover-subtitle="coverSubtitle = $event"
        @apply-cover="applyCover"
      />

      <AlbumStepContents
        v-if="currentStep === 4"
        :can-add-contents="canAddContents"
        :is-adding-contents="isAddingContents"
        :is-contents-added="isContentsAdded"
        @add-contents="addContents"
      />

      <AlbumStepFinalize
        v-if="currentStep === 5"
        :can-finalize="canFinalize"
        :is-finalizing="isFinalizing"
        :is-finalized="isFinalized"
        @finalize-book="finalizeBook"
      />

      <AlbumStepOrder
        v-if="currentStep === 6"
        :finalized-book-uid-in-flow="finalizedBookUidInFlow"
        :finalized-book-title-in-flow="finalizedBookTitleInFlow"
        @go-to-order="goToOrder"
      />

      <AlbumStepNav
        :current-step="currentStep"
        :max-navigable-step="maxNavigableStep"
        @prev="goPrevStep"
        @next="goNextStep"
      />

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAlbumStore } from '../store';
import * as albumApi from '../api/albumApi';
import AlbumStepHeader from '../components/steps/AlbumStepHeader.vue';
import AlbumStepDraft from '../components/steps/AlbumStepDraft.vue';
import AlbumStepPhotos, { type UploadedPhotoItem } from '../components/steps/AlbumStepPhotos.vue';
import AlbumStepCover from '../components/steps/AlbumStepCover.vue';
import AlbumStepContents from '../components/steps/AlbumStepContents.vue';
import AlbumStepFinalize from '../components/steps/AlbumStepFinalize.vue';
import AlbumStepOrder from '../components/steps/AlbumStepOrder.vue';
import AlbumStepNav from '../components/steps/AlbumStepNav.vue';

const route = useRoute();
const router = useRouter();
const albumStore = useAlbumStore();

const fetchError = ref<string | null>(null);
const errorMessage = ref<string | null>(null);
const bookTitle = ref('');
const bookUid = ref<string | null>(null);

const isCreatingDraft = ref(false);
const isUploadingPhoto = ref(false);
const isApplyingCover = ref(false);
const isAddingContents = ref(false);
const isFinalizing = ref(false);

const isCoverApplied = ref(false);
const isContentsAdded = ref(false);
const isFinalized = ref(false);

const coverFrontFileName = ref<string | null>(null);
const coverFrontPreviewUrl = ref<string | null>(null);
const coverSubtitle = ref('');

const activityPhotos = ref<Record<number, UploadedPhotoItem[]>>({});
const finalizedBookUidInFlow = ref<string | null>(null);
const finalizedBookTitleInFlow = ref<string>('');
const MIN_ACTIVITY_COUNT = 24;
const currentStep = ref(1);
const STEP_ITEMS = [
  { step: 1, label: '1. Draft' },
  { step: 2, label: '2. 사진' },
  { step: 3, label: '3. 표지' },
  { step: 4, label: '4. 내지' },
  { step: 5, label: '5. 최종화' },
  { step: 6, label: '6. 주문' },
] as const;

const albumId = computed(() => Number(route.params.id));

const canCreateDraft = computed(() => {
  const count = albumStore.currentAlbum?.selectedActivities.length ?? 0;
  return count >= MIN_ACTIVITY_COUNT && bookTitle.value.trim().length > 0;
});

const canApplyCover = computed(() => !!bookUid.value && !!coverFrontFileName.value);

const canAddContents = computed(() => {
  if (!bookUid.value || !isCoverApplied.value || !albumStore.currentAlbum) return false;
  return albumStore.currentAlbum.selectedActivities.length >= MIN_ACTIVITY_COUNT;
});

const canFinalize = computed(() => !!bookUid.value && isCoverApplied.value && isContentsAdded.value);

const maxNavigableStep = computed(() => {
  if (isFinalized.value) return 6;
  if (isContentsAdded.value) return 5;
  if (isCoverApplied.value) return 4;
  if (bookUid.value) return 3;
  return 1;
});

const goToStep = (step: number) => {
  if (step < 1 || step > maxNavigableStep.value) return;
  currentStep.value = step;
};

const goPrevStep = () => {
  if (currentStep.value <= 1) return;
  currentStep.value -= 1;
};

const goNextStep = () => {
  if (currentStep.value >= maxNavigableStep.value) return;
  currentStep.value += 1;
};

const loadAlbumData = async () => {
  if (!albumId.value) return;
  fetchError.value = null;
  try {
    await albumStore.fetchAlbum(albumId.value);
    if (!albumStore.currentAlbum) return;

    if (!bookTitle.value) bookTitle.value = albumStore.currentAlbum.title;
    if (!coverSubtitle.value) coverSubtitle.value = albumStore.currentAlbum.title;
    if (albumStore.currentAlbum.bookUid) bookUid.value = albumStore.currentAlbum.bookUid;
  } catch (err: any) {
    fetchError.value = err?.message || '앨범 조회에 실패했습니다.';
  }
};

const replacePreview = (file: File) => {
  const nextUrl = URL.createObjectURL(file);
  if (coverFrontPreviewUrl.value) URL.revokeObjectURL(coverFrontPreviewUrl.value);
  coverFrontPreviewUrl.value = nextUrl;
};

const createDraftBook = async () => {
  if (!albumId.value || isCreatingDraft.value || !canCreateDraft.value) return;
  errorMessage.value = null;

  isCreatingDraft.value = true;
  try {
    await albumStore.updateAlbum(albumId.value, { title: bookTitle.value.trim() });
    const result = await albumStore.createBookDraft(albumId.value, { title: bookTitle.value.trim() });
    bookUid.value = result.bookUid;
    isCoverApplied.value = false;
    isContentsAdded.value = false;
    isFinalized.value = false;
    finalizedBookUidInFlow.value = null;
    finalizedBookTitleInFlow.value = '';
    coverSubtitle.value = bookTitle.value.trim();
    await loadAlbumData();
    currentStep.value = 2;
  } catch (err: any) {
    errorMessage.value = err?.message || '책 Draft 생성에 실패했습니다.';
  } finally {
    isCreatingDraft.value = false;
  }
};

const uploadSinglePhoto = async (file: File) => {
  if (!albumId.value) return null;
  const uploaded = await albumApi.uploadBookPhoto(albumId.value, file);
  return uploaded.fileName as string;
};

const onCoverFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file || !bookUid.value) return;

  errorMessage.value = null;
  isUploadingPhoto.value = true;
  try {
    const fileName = await uploadSinglePhoto(file);
    if (!fileName) return;
    replacePreview(file);
    coverFrontFileName.value = fileName;
  } catch (err: any) {
    errorMessage.value = err?.message || '표지 사진 업로드에 실패했습니다.';
  } finally {
    isUploadingPhoto.value = false;
    input.value = '';
  }
};

const onActivityFilesChange = async (albumActivityId: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = input.files ? Array.from(input.files) : [];
  if (files.length === 0 || !bookUid.value) return;

  errorMessage.value = null;
  isUploadingPhoto.value = true;
  try {
    const uploadedItems: UploadedPhotoItem[] = [];
    for (const file of files) {
      const fileName = await uploadSinglePhoto(file);
      if (!fileName) continue;
      uploadedItems.push({ fileName, previewUrl: URL.createObjectURL(file) });
    }
    const prev = activityPhotos.value[albumActivityId] ?? [];
    activityPhotos.value[albumActivityId] = [...prev, ...uploadedItems];
  } catch (err: any) {
    errorMessage.value = err?.message || '활동 사진 업로드에 실패했습니다.';
  } finally {
    isUploadingPhoto.value = false;
    input.value = '';
  }
};

const applyCover = async () => {
  if (!albumId.value || !canApplyCover.value || isApplyingCover.value) return;
  errorMessage.value = null;

  isApplyingCover.value = true;
  try {
    await albumApi.applyBookCover(albumId.value, {
      coverPhotoFileName: coverFrontFileName.value!,
      subtitle: coverSubtitle.value.trim() || bookTitle.value.trim(),
    });
    isCoverApplied.value = true;
    currentStep.value = 4;
  } catch (err: any) {
    errorMessage.value = err?.message || '표지 추가에 실패했습니다.';
  } finally {
    isApplyingCover.value = false;
  }
};

const addContents = async () => {
  if (!albumId.value || !albumStore.currentAlbum || !canAddContents.value || isAddingContents.value) return;
  errorMessage.value = null;

  isAddingContents.value = true;
  try {
    const pages = albumStore.currentAlbum.selectedActivities.map((activity) => ({
      albumActivityId: activity.albumActivityId,
      photoFileNames: (activityPhotos.value[activity.albumActivityId] ?? []).map((item) => item.fileName),
    }));
    await albumApi.addBookContents(albumId.value, { pages });
    isContentsAdded.value = true;
    currentStep.value = 5;
  } catch (err: any) {
    errorMessage.value = err?.message || '내지 추가에 실패했습니다.';
  } finally {
    isAddingContents.value = false;
  }
};

const finalizeBook = async () => {
  if (!albumId.value || !canFinalize.value || isFinalizing.value) return;
  errorMessage.value = null;

  isFinalizing.value = true;
  try {
    const result = await albumStore.finalizeBook(albumId.value);
    if (result?.bookUid) {
      finalizedBookUidInFlow.value = result.bookUid;
      finalizedBookTitleInFlow.value = (bookTitle.value.trim() || albumStore.currentAlbum?.title || '').trim();
    }
    isFinalized.value = true;
    currentStep.value = 6;
  } catch (err: any) {
    errorMessage.value = err?.message || '최종화에 실패했습니다.';
  } finally {
    isFinalizing.value = false;
  }
};

const goToOrder = () => {
  if (!albumId.value || !finalizedBookUidInFlow.value) return;
  router.push({
    name: 'order-list',
    params: { albumId: albumId.value },
    query: { bookUid: finalizedBookUidInFlow.value },
  });
};

const revokeAllObjectUrls = () => {
  if (coverFrontPreviewUrl.value) URL.revokeObjectURL(coverFrontPreviewUrl.value);
  Object.values(activityPhotos.value).forEach((items) => items.forEach((item) => URL.revokeObjectURL(item.previewUrl)));
};

onMounted(loadAlbumData);
onBeforeUnmount(revokeAllObjectUrls);
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

.card {
  background: var(--color-white);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.album-title {
  margin: 0 0 6px;
}

.section-title {
  margin: 0 0 12px;
}

.stepper {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.step-chip {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 10px 16px;
  border: 1px solid var(--color-border-cream);
}

.step-chip.active {
  background: var(--color-terracotta);
  color: var(--color-white);
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

.state-message {
  color: var(--color-olive-gray);
  margin: 6px 0;
}

.success-message {
  margin: 8px 0 0;
  color: #2c6d47;
}

.error-message {
  color: var(--color-error);
}

.btn-primary {
  background: var(--color-terracotta);
  color: var(--color-white);
  padding: 12px 16px;
  font-size: 0.95rem;
  min-height: 44px;
}

.btn-secondary {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 10px 14px;
  font-size: 0.9rem;
  min-height: 42px;
}

.btn-primary:disabled,
.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.thumb-row {
  margin: 12px 0;
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
  object-fit: cover;
}

.activity-list {
  display: grid;
  gap: 12px;
}

.activity-item {
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  padding: 12px;
}

.activity-meta {
  display: grid;
  gap: 4px;
  margin-bottom: 6px;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.preview-item {
  width: 96px;
  display: grid;
  gap: 4px;
}

.preview-image {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  object-fit: cover;
}

.finalized-summary {
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  padding: 10px 12px;
  margin: 10px 0 12px;
  background: var(--color-white);
}

.step-nav,
.album-detail-page :deep(.step-nav) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.album-detail-page :deep(.card) {
  background: var(--color-white);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 20px;
}

.album-detail-page :deep(.section-title) {
  margin: 0 0 12px;
}

.album-detail-page :deep(.field) {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.album-detail-page :deep(.field-grid) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.album-detail-page :deep(.state-message) {
  color: var(--color-olive-gray);
  margin: 6px 0;
}

.album-detail-page :deep(.success-message) {
  color: #2c6d47;
  margin: 8px 0 0;
}

.album-detail-page :deep(.error-message) {
  color: var(--color-error);
}

.album-detail-page :deep(.btn-primary) {
  background: var(--color-terracotta);
  color: var(--color-ivory);
  padding: 12px 16px;
  min-height: 44px;
  font-size: 0.95rem;
}

.album-detail-page :deep(.btn-secondary) {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 10px 14px;
  min-height: 42px;
  font-size: 0.9rem;
}

.album-detail-page :deep(.btn-primary:disabled),
.album-detail-page :deep(.btn-secondary:disabled) {
  opacity: 0.6;
  cursor: not-allowed;
}

.album-detail-page :deep(.stepper) {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.album-detail-page :deep(.step-chip) {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 10px 16px;
  border: 1px solid var(--color-border-cream);
  min-height: 42px;
  border-radius: 10px;
}

.album-detail-page :deep(.step-chip.active) {
  background: var(--color-terracotta);
  color: var(--color-white);
}

.album-detail-page :deep(.thumb-row) {
  margin: 12px 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.album-detail-page :deep(.thumb-col) {
  display: grid;
  gap: 8px;
}

.album-detail-page :deep(.thumb) {
  width: 100%;
  max-width: 360px;
  height: 220px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid var(--color-border-cream);
  background: #f4f4f4;
}

.album-detail-page :deep(.activity-list) {
  display: grid;
  gap: 12px;
}

.album-detail-page :deep(.activity-item) {
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  padding: 12px;
  background: var(--color-ivory);
}

.album-detail-page :deep(.activity-meta) {
  display: grid;
  gap: 4px;
  margin-bottom: 8px;
}

.album-detail-page :deep(.preview-list) {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.album-detail-page :deep(.preview-item) {
  width: 120px;
  display: grid;
  gap: 4px;
  align-content: start;
}

.album-detail-page :deep(.preview-image) {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid var(--color-border-cream);
  background: #f4f4f4;
}

.album-detail-page :deep(.preview-item .state-message) {
  font-size: 11px;
  line-height: 1.3;
  word-break: break-all;
}

@media (max-width: 768px) {
  .field-grid,
  .thumb-row,
  .album-detail-page :deep(.field-grid),
  .album-detail-page :deep(.thumb-row) {
    grid-template-columns: 1fr;
  }

  .album-detail-page :deep(.thumb) {
    max-width: 100%;
    height: 200px;
  }
}
</style>
