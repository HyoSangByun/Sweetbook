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

      <section class="card">
        <h3 class="section-title">단계별 생성</h3>
        <div class="stepper">
          <button
            v-for="step in STEP_ITEMS"
            :key="step.step"
            type="button"
            class="step-chip"
            :class="{ active: currentStep === step.step }"
            :disabled="step.step > maxNavigableStep"
            @click="goToStep(step.step)"
          >
            {{ step.label }}
          </button>
        </div>
      </section>

      <section class="card" v-if="currentStep === 1">
        <h3 class="section-title">1단계: 책 Draft 생성</h3>
        <p class="state-message">활동 선택 후 SweetBook의 POST /books를 먼저 호출합니다.</p>
        <label class="field">
          <span>책 제목</span>
          <input v-model="bookTitle" type="text" maxlength="255" />
        </label>
        <p class="state-message">활동 최소 선택 수: 24개</p>
        <p class="state-message">고정 판형: <strong>SQUAREBOOK_HC</strong></p>
        <button class="btn-primary" :disabled="isCreatingDraft || !canCreateDraft" @click="createDraftBook">
          {{ isCreatingDraft ? '생성 중...' : '책 Draft 생성' }}
        </button>
        <p v-if="bookUid" class="success-message">생성된 bookUid: {{ bookUid }}</p>
      </section>

      <section class="card" v-if="currentStep === 2">
        <h3 class="section-title">2단계: 사진 업로드</h3>
        <p class="state-message">bookUid 기준으로 POST /books/{bookUid}/photos를 수행합니다.</p>

        <div class="field-grid">
          <label class="field">
            <span>표지 메인 사진 (필수)</span>
            <input type="file" accept="image/*" @change="onCoverFileChange($event)" :disabled="!bookUid || isUploadingPhoto" />
          </label>
        </div>

        <div class="thumb-row">
          <div class="thumb-col">
            <p>표지 메인</p>
            <img v-if="coverFrontPreviewUrl" :src="coverFrontPreviewUrl" class="thumb" alt="표지 메인" />
            <p v-if="coverFrontFileName" class="state-message">{{ coverFrontFileName }}</p>
          </div>
        </div>

        <div v-if="albumStore.currentAlbum.selectedActivities.length > 0" class="activity-list">
          <article v-for="activity in albumStore.currentAlbum.selectedActivities" :key="activity.albumActivityId" class="activity-item">
            <div class="activity-meta">
              <strong>{{ formatDate(activity.activityDateTime) }}</strong>
              <span>{{ activity.activityName }}</span>
              <span>{{ activity.distanceKm.toFixed(2) }}km</span>
            </div>
            <label class="field">
              <span>활동 사진 (선택, 다중 업로드)</span>
              <input
                type="file"
                accept="image/*"
                multiple
                @change="onActivityFilesChange(activity.albumActivityId, $event)"
                :disabled="!bookUid || isUploadingPhoto"
              />
            </label>
            <div class="preview-list">
              <div v-for="item in activityPhotos[activity.albumActivityId] || []" :key="item.fileName" class="preview-item">
                <img :src="item.previewUrl" class="preview-image" alt="활동 사진" />
                <span class="state-message">{{ item.fileName }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="card" v-if="currentStep === 3">
        <h3 class="section-title">3단계: 표지 적용</h3>
        <p class="state-message">고정 템플릿 UID: <strong>4Fy1mpIlm1ek</strong></p>
        <label class="field">
          <span>표지 부제목 (기본값: 책 제목)</span>
          <input v-model="coverSubtitle" type="text" maxlength="255" :disabled="!bookUid" />
        </label>
        <button class="btn-primary" :disabled="!canApplyCover || isApplyingCover" @click="applyCover">
          {{ isApplyingCover ? '적용 중...' : '표지 추가' }}
        </button>
        <p v-if="isCoverApplied" class="success-message">표지 적용 완료</p>
      </section>

      <section class="card" v-if="currentStep === 4">
        <h3 class="section-title">4단계: 내지 추가</h3>
        <p class="state-message">고정 템플릿 UID: <strong>3T09l6GEd0AL</strong></p>
        <p class="state-message">사진을 선택하지 않으면 기본 이미지(https://placehold.co/300x200.jpg)가 자동 적용됩니다.</p>
        <button class="btn-primary" :disabled="!canAddContents || isAddingContents" @click="addContents">
          {{ isAddingContents ? '추가 중...' : '내지 추가' }}
        </button>
        <p v-if="isContentsAdded" class="success-message">내지 추가 완료</p>
      </section>

      <section class="card" v-if="currentStep === 5">
        <h3 class="section-title">5단계: 최종화</h3>
        <button class="btn-primary" :disabled="!canFinalize || isFinalizing" @click="finalizeBook">
          {{ isFinalizing ? '최종화 중...' : '최종화 완료' }}
        </button>
        <p v-if="isFinalized" class="success-message">최종화 완료</p>
      </section>

      <section class="card" v-if="currentStep === 6">
        <h3 class="section-title">6단계: 주문 진행</h3>
        <p class="state-message">이번에 최종화된 책 정보입니다.</p>
        <div v-if="finalizedBookUidInFlow" class="finalized-summary">
          <p><strong>{{ finalizedBookTitleInFlow || '(제목 없음)' }}</strong></p>
          <p class="state-message">bookUid: {{ finalizedBookUidInFlow }}</p>
        </div>
        <p v-else class="error-message">이번 생성에서 최종화된 책이 없어 주문으로 이동할 수 없습니다.</p>
        <button class="btn-primary" :disabled="!finalizedBookUidInFlow" @click="goToOrder">
          주문하러 가기
        </button>
      </section>

      <section class="card step-nav">
        <button type="button" class="btn-secondary" :disabled="currentStep <= 1" @click="goPrevStep">이전 단계</button>
        <button type="button" class="btn-secondary" :disabled="currentStep >= maxNavigableStep" @click="goNextStep">다음 단계</button>
      </section>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAlbumStore } from '../store';
import * as albumApi from '../api/albumApi';

type UploadedPhotoItem = {
  fileName: string;
  previewUrl: string;
};

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
  { step: 6, label: '6. 책 선택' },
] as const;

const albumId = computed(() => Number(route.params.id));

const canCreateDraft = computed(() => {
  const count = albumStore.currentAlbum?.selectedActivities.length ?? 0;
  return count >= MIN_ACTIVITY_COUNT && bookTitle.value.trim().length > 0;
});

const canApplyCover = computed(() => {
  return !!bookUid.value && !!coverFrontFileName.value;
});

const canAddContents = computed(() => {
  if (!bookUid.value || !isCoverApplied.value || !albumStore.currentAlbum) return false;
  if (albumStore.currentAlbum.selectedActivities.length < MIN_ACTIVITY_COUNT) return false;
  return true;
});

const canFinalize = computed(() => {
  return !!bookUid.value && isCoverApplied.value && isContentsAdded.value;
});

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
    if (albumStore.currentAlbum) {
      if (!bookTitle.value) {
        bookTitle.value = albumStore.currentAlbum.title;
      }
      if (!coverSubtitle.value) {
        coverSubtitle.value = albumStore.currentAlbum.title;
      }
      if (albumStore.currentAlbum.bookUid) {
        bookUid.value = albumStore.currentAlbum.bookUid;
      }
    }
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
  if ((albumStore.currentAlbum?.selectedActivities.length ?? 0) < MIN_ACTIVITY_COUNT) {
    errorMessage.value = `활동은 최소 ${MIN_ACTIVITY_COUNT}개 이상 선택해야 합니다.`;
    return;
  }
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
    errorMessage.value = err?.message || '책 draft 생성에 실패했습니다.';
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
      uploadedItems.push({
        fileName,
        previewUrl: URL.createObjectURL(file),
      });
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
  const targetBookUid = finalizedBookUidInFlow.value;
  if (!albumId.value || !targetBookUid) return;
  router.push({
    name: 'order-list',
    params: { albumId: albumId.value },
    query: { bookUid: targetBookUid },
  });
};

const formatDate = (value: string) => {
  return new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
};

const revokeAllObjectUrls = () => {
  if (coverFrontPreviewUrl.value) URL.revokeObjectURL(coverFrontPreviewUrl.value);
  Object.values(activityPhotos.value).forEach((items) => {
    items.forEach((item) => URL.revokeObjectURL(item.previewUrl));
  });
};

onMounted(async () => {
  await loadAlbumData();
});

onBeforeUnmount(() => {
  revokeAllObjectUrls();
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

.card {
  background: var(--color-white);
  border: 1px solid var(--color-border-cream);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.card.disabled {
  opacity: 0.65;
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
  gap: 8px;
}

.step-chip {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
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
  padding: 10px 14px;
}

.btn-secondary {
  background: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
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

.step-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .field-grid,
  .thumb-row {
    grid-template-columns: 1fr;
  }
}
</style>
