<template>
  <div class="album-detail-page">
    <header class="page-header">
      <div class="container header-content">
        <router-link to="/" class="btn-back">Back to activities</router-link>
        <h1 class="logo">SweetBook</h1>
        <button @click="handleLogout" class="btn-logout">Logout</button>
      </div>
    </header>

    <main class="container" v-if="albumStore.currentAlbum && !fetchError">
      <section class="card">
        <h2 class="album-title">{{ albumStore.currentAlbum.title }}</h2>
        <p class="state-message">{{ albumStore.currentAlbum.month }}</p>
      </section>

      <section class="card">
        <h3 class="section-title">1단계 · 책 Draft 생성</h3>
        <p class="state-message">활동 선택이 끝나면 먼저 SweetBook의 POST /books를 호출합니다.</p>
        <label class="field">
          <span>Book title</span>
          <input v-model="bookTitle" type="text" maxlength="255" />
        </label>
        <p class="state-message">활동 최소 선택 수: 24개</p>
        <p class="state-message">고정 판형: <strong>SQUAREBOOK_HC</strong></p>
        <button class="btn-primary" :disabled="isCreatingDraft || !canCreateDraft" @click="createDraftBook">
          {{ isCreatingDraft ? '생성 중...' : '책 Draft 생성' }}
        </button>
        <p v-if="bookUid" class="success-message">생성된 bookUid: {{ bookUid }}</p>
      </section>

      <section class="card" :class="{ disabled: !bookUid }">
        <h3 class="section-title">2단계 · 사진 업로드</h3>
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
            <img v-if="coverFrontPreviewUrl" :src="coverFrontPreviewUrl" class="thumb" alt="cover front" />
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
              <span>활동 사진 (필수, 다중 업로드)</span>
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
                <img :src="item.previewUrl" class="preview-image" alt="activity photo" />
                <span class="state-message">{{ item.fileName }}</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="card" :class="{ disabled: !bookUid }">
        <h3 class="section-title">3단계 · 표지 적용</h3>
        <p class="state-message">고정 템플릿 UID: <strong>4Fy1mpIlm1ek</strong></p>
        <label class="field">
          <span>표지 subtitle (기본값: Book title)</span>
          <input v-model="coverSubtitle" type="text" maxlength="255" :disabled="!bookUid" />
        </label>
        <button class="btn-primary" :disabled="!canApplyCover || isApplyingCover" @click="applyCover">
          {{ isApplyingCover ? '적용 중...' : '표지 추가' }}
        </button>
        <p v-if="isCoverApplied" class="success-message">표지 적용 완료</p>
      </section>

      <section class="card" :class="{ disabled: !isCoverApplied }">
        <h3 class="section-title">4단계 · 내지 추가</h3>
        <p class="state-message">고정 템플릿 UID: <strong>3T09l6GEd0AL</strong></p>
        <p class="state-message">모든 선택 활동에 사진이 최소 1장 있어야 내지 추가가 가능합니다.</p>
        <button class="btn-primary" :disabled="!canAddContents || isAddingContents" @click="addContents">
          {{ isAddingContents ? '추가 중...' : '내지 추가' }}
        </button>
        <p v-if="isContentsAdded" class="success-message">내지 추가 완료</p>
      </section>

      <section class="card" :class="{ disabled: !isContentsAdded }">
        <h3 class="section-title">5단계 · 최종화</h3>
        <button class="btn-primary" :disabled="!canFinalize || isFinalizing" @click="finalizeBook">
          {{ isFinalizing ? '최종화 중...' : '최종화 완료' }}
        </button>
        <p v-if="isFinalized" class="success-message">최종화 완료</p>
      </section>

      <section class="card">
        <h3 class="section-title">생성된 책 목록 (GET /books)</h3>
        <button class="btn-secondary" @click="loadCreatedBooks" :disabled="isLoadingBooks">
          {{ isLoadingBooks ? '조회 중...' : '목록 새로고침' }}
        </button>

        <div v-if="createdBooks.length === 0" class="state-message">현재 앨범에서 생성된 책이 없습니다.</div>
        <div v-else class="book-list">
          <label v-for="book in createdBooks" :key="book.bookUid" class="book-item">
            <input type="radio" name="selectedBook" :value="book.bookUid" v-model="selectedBookUid" />
            <div>
              <p><strong>{{ book.title || '(제목 없음)' }}</strong></p>
              <p class="state-message">bookUid: {{ book.bookUid }}</p>
              <p class="state-message">status: {{ book.status }}</p>
            </div>
          </label>
        </div>

        <button class="btn-primary" :disabled="!selectedBookUid" @click="goToOrder">
          선택한 책으로 주문하러 가기
        </button>
      </section>

      <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../auth/store';
import { useAlbumStore } from '../store';
import * as albumApi from '../api/albumApi';

type UploadedPhotoItem = {
  fileName: string;
  previewUrl: string;
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
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
const isLoadingBooks = ref(false);

const isCoverApplied = ref(false);
const isContentsAdded = ref(false);
const isFinalized = ref(false);

const coverFrontFileName = ref<string | null>(null);
const coverFrontPreviewUrl = ref<string | null>(null);
const coverSubtitle = ref('');

const activityPhotos = ref<Record<number, UploadedPhotoItem[]>>({});
const createdBooks = ref<Array<{ bookUid: string; title?: string; status?: number }>>([]);
const selectedBookUid = ref<string | null>(null);
const MIN_ACTIVITY_COUNT = 24;

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
  return albumStore.currentAlbum.selectedActivities.every((activity) => {
    const photos = activityPhotos.value[activity.albumActivityId] ?? [];
    return photos.length > 0;
  });
});

const canFinalize = computed(() => {
  return !!bookUid.value && isCoverApplied.value && isContentsAdded.value;
});

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

const loadCreatedBooks = async () => {
  if (!albumId.value) return;
  isLoadingBooks.value = true;
  try {
    const books = await albumApi.getAlbumBooks(albumId.value);
    createdBooks.value = books.map((book: any) => ({
      bookUid: String(book.bookUid),
      title: book.title ? String(book.title) : undefined,
      status: typeof book.status === 'number' ? book.status : undefined,
    }));
    if (!selectedBookUid.value && createdBooks.value.length > 0) {
      selectedBookUid.value = createdBooks.value[0].bookUid;
    }
  } catch (err: any) {
    errorMessage.value = err?.message || '책 목록 조회에 실패했습니다.';
  } finally {
    isLoadingBooks.value = false;
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
    coverSubtitle.value = bookTitle.value.trim();
    await loadAlbumData();
    await loadCreatedBooks();
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
  } catch (err: any) {
    errorMessage.value = err?.message || '표지 추가에 실패했습니다.';
  } finally {
    isApplyingCover.value = false;
  }
};

const addContents = async () => {
  if (!albumId.value || !albumStore.currentAlbum || !canAddContents.value || isAddingContents.value) return;
  errorMessage.value = null;
  const activityWithoutPhoto = albumStore.currentAlbum.selectedActivities.find((activity) => {
    const photos = activityPhotos.value[activity.albumActivityId] ?? [];
    return photos.length === 0;
  });
  if (activityWithoutPhoto) {
    errorMessage.value = '모든 선택 활동에 사진을 최소 1장씩 업로드해야 합니다.';
    return;
  }
  isAddingContents.value = true;
  try {
    const pages = albumStore.currentAlbum.selectedActivities.map((activity) => ({
      albumActivityId: activity.albumActivityId,
      photoFileNames: (activityPhotos.value[activity.albumActivityId] ?? []).map((item) => item.fileName),
    }));
    await albumApi.addBookContents(albumId.value, { pages });
    isContentsAdded.value = true;
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
      selectedBookUid.value = result.bookUid;
    }
    isFinalized.value = true;
    await loadCreatedBooks();
  } catch (err: any) {
    errorMessage.value = err?.message || '최종화에 실패했습니다.';
  } finally {
    isFinalizing.value = false;
  }
};

const goToOrder = () => {
  if (!albumId.value || !selectedBookUid.value) return;
  router.push({
    name: 'order-list',
    params: { albumId: albumId.value },
    query: { bookUid: selectedBookUid.value },
  });
};

const formatDate = (value: string) => {
  return new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
};

const handleLogout = () => {
  authStore.logout();
  router.push({ name: 'login' });
};

const revokeAllObjectUrls = () => {
  if (coverFrontPreviewUrl.value) URL.revokeObjectURL(coverFrontPreviewUrl.value);
  Object.values(activityPhotos.value).forEach((items) => {
    items.forEach((item) => URL.revokeObjectURL(item.previewUrl));
  });
};

onMounted(async () => {
  await loadAlbumData();
  await loadCreatedBooks();
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

.card.disabled {
  opacity: 0.65;
}

.album-title {
  margin: 0 0 6px;
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

.book-list {
  display: grid;
  gap: 8px;
  margin: 12px 0;
}

.book-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  border: 1px solid var(--color-border-cream);
  border-radius: 10px;
  padding: 10px;
}

@media (max-width: 768px) {
  .field-grid,
  .thumb-row {
    grid-template-columns: 1fr;
  }
}
</style>
