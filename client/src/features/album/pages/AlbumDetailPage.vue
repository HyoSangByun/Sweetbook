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
      <section class="album-info-section">
        <div class="month-badge">{{ albumStore.currentAlbum.month }}</div>
        <h2 class="album-title">{{ albumStore.currentAlbum.title }}</h2>
      </section>

      <section class="book-config card">
        <h3 class="section-title">Book configuration</h3>

        <div class="field-grid">
          <label class="field">
            <span>Book title</span>
            <input v-model="bookForm.title" type="text" required maxlength="255" />
          </label>

          <label class="field">
            <span>Book spec</span>
            <select v-model="bookForm.bookSpecUid" :disabled="isBookSpecsLoading">
              <option value="" disabled>Select a book spec</option>
              <option v-for="spec in bookSpecs" :key="spec.bookSpecUid" :value="spec.bookSpecUid">
                {{ specLabel(spec) }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>Cover template</span>
            <select v-model="bookForm.coverTemplateUid" :disabled="isTemplatesLoading || coverTemplateOptions.length === 0">
              <option value="" disabled>Select a cover template</option>
              <option
                v-for="option in coverTemplateOptions"
                :key="option.templateUid"
                :value="option.templateUid"
                :disabled="!!option.disabledReason"
              >
                {{ templateLabel(option) }}{{ option.disabledReason ? ' (unavailable)' : '' }}
              </option>
            </select>
            <small class="field-help">{{ templateHelpText('cover') }}</small>
          </label>

          <label class="field">
            <span>Content template</span>
            <select v-model="bookForm.contentTemplateUid" :disabled="isTemplatesLoading || contentTemplateOptions.length === 0">
              <option value="" disabled>Select a content template</option>
              <option
                v-for="option in contentTemplateOptions"
                :key="option.templateUid"
                :value="option.templateUid"
                :disabled="!!option.disabledReason"
              >
                {{ templateLabel(option) }}{{ option.disabledReason ? ' (unavailable)' : '' }}
              </option>
            </select>
            <small class="field-help">{{ templateHelpText('content') }}</small>
          </label>
        </div>

        <div class="state-stack">
          <p v-if="configLoadError" class="error-message">{{ configLoadError }}</p>
          <p v-if="isBookSpecsLoading" class="state-message">Loading book specs...</p>
          <p v-else-if="bookSpecs.length === 0" class="state-message">No book specs available.</p>
          <p v-if="isTemplatesLoading" class="state-message">Loading templates...</p>
          <p v-else-if="bookForm.bookSpecUid && coverTemplateOptions.length === 0 && contentTemplateOptions.length === 0" class="state-message">
            No templates available for the selected book spec.
          </p>
        </div>

        <div v-if="selectedBookSpecDetail" class="spec-detail">
          <h4>Selected spec details</h4>
          <p>Name: {{ selectedBookSpecDetail.name || selectedBookSpecDetail.bookSpecUid }}</p>
          <p>
            Size:
            {{ selectedBookSpecDetail.innerTrimWidthMm ?? '-' }} x {{ selectedBookSpecDetail.innerTrimHeightMm ?? '-' }} mm
          </p>
          <p>
            Page rules:
            {{ selectedBookSpecDetail.pageMin ?? '-' }} ~ {{ selectedBookSpecDetail.pageMax ?? '-' }}
            (step {{ selectedBookSpecDetail.pageIncrement ?? '-' }})
          </p>
          <p>
            Pricing:
            base {{ selectedBookSpecDetail.priceBase ?? '-' }},
            +{{ selectedBookSpecDetail.pricePerIncrement ?? '-' }} per increment
          </p>
        </div>
      </section>

      <section class="activities-section card">
        <h3 class="section-title">Selected activities ({{ albumStore.currentAlbum.selectedActivityCount }})</h3>
        <div v-if="albumStore.currentAlbum.selectedActivities.length === 0" class="empty-state">No selected activities.</div>
        <div v-else class="activity-list">
          <div v-for="activity in albumStore.currentAlbum.selectedActivities" :key="activity.albumActivityId" class="activity-item">
            <div class="activity-meta">
              <strong>{{ formatDate(activity.activityDateTime) }}</strong>
              <span>{{ activity.activityName }}</span>
              <span>{{ activity.distanceKm.toFixed(2) }}km</span>
            </div>
            <div class="photo-section">
              <label class="file-label">
                Attach photo (optional)
                <input type="file" accept="image/*" @change="onPhotoChange(activity.activityId, $event)" />
              </label>
              <div v-if="photoPreviewByActivity[activity.activityId]" class="preview-box">
                <img :src="photoPreviewByActivity[activity.activityId]" alt="Photo preview" class="preview-image" />
                <button type="button" class="btn-remove" @click="clearPhoto(activity.activityId)">Remove</button>
              </div>
            </div>
            <button class="btn-deselect" @click="handleDeselect(activity.activityId)">Deselect activity</button>
          </div>
        </div>
      </section>

      <section class="preview-section card">
        <div class="preview-actions">
          <button class="btn-primary" @click="openPreview" :disabled="isPreviewLoading || isGenerating || isTemplatesLoading || isBookSpecsLoading">
            {{ isPreviewLoading ? 'Calculating...' : 'Preview' }}
          </button>
        </div>
        <p v-if="previewError" class="error-message">{{ previewError }}</p>
        <p v-if="generateError" class="error-message">{{ generateError }}</p>

        <div v-if="showPreview" class="preview-summary">
          <h4>Preview summary</h4>
          <p>Title: {{ bookForm.title }}</p>
          <p>Book spec: {{ selectedBookSpecLabel }}</p>
          <p>Estimated pages: {{ estimate?.estimatedPageCount ?? estimatedPageCount }}</p>
          <p v-if="estimate?.totalAmount != null">Estimated total: {{ formatCurrency(estimate.totalAmount, estimate.currency || 'KRW') }}</p>
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
            {{ isGenerating ? 'Generating...' : 'Confirm and proceed to order' }}
          </button>
        </div>
      </section>
    </main>

    <div v-else-if="fetchError" class="error-state">
      <p class="error-message">{{ fetchError }}</p>
      <button @click="loadAlbumData" class="btn-primary">Retry</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../auth/store';
import { useAlbumStore } from '../store';
import * as albumApi from '../api/albumApi';

type BookSpecSummary = {
  bookSpecUid: string;
  name?: string;
  pageMin?: number;
  pageMax?: number;
  pageIncrement?: number;
};

type BookSpecDetail = BookSpecSummary & {
  innerTrimWidthMm?: number;
  innerTrimHeightMm?: number;
  priceBase?: number;
  pricePerIncrement?: number;
};

type TemplateSummary = {
  templateUid: string;
  name?: string;
  title?: string;
};

type TemplateOption = TemplateSummary & {
  requiresImage: boolean;
  disabledReason: string | null;
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const albumStore = useAlbumStore();

const fetchError = ref<string | null>(null);
const configLoadError = ref<string | null>(null);
const previewError = ref<string | null>(null);
const generateError = ref<string | null>(null);

const isBookSpecsLoading = ref(false);
const isBookSpecDetailLoading = ref(false);
const isTemplatesLoading = ref(false);
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

const bookSpecs = ref<BookSpecSummary[]>([]);
const selectedBookSpecDetail = ref<BookSpecDetail | null>(null);
const coverTemplates = ref<TemplateSummary[]>([]);
const contentTemplates = ref<TemplateSummary[]>([]);
const templateDetailsByUid = ref<Record<string, any>>({});

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
const hasLocalPhoto = computed(() => Object.values(photoFileByActivity.value).some((file) => file instanceof File));
const hasAnyPhoto = computed(() => hasLocalPhoto.value || Boolean(albumStore.currentAlbum?.hasPhoto));

const selectedBookSpecLabel = computed(() => {
  if (selectedBookSpecDetail.value?.name) return selectedBookSpecDetail.value.name;
  return selectedBookSpecDetail.value?.bookSpecUid || bookForm.bookSpecUid || '-';
});

const templateLabel = (template: TemplateSummary) => template.name || template.title || template.templateUid;

const specLabel = (spec: BookSpecSummary) => {
  const namePart = spec.name ? `${spec.name} ` : '';
  if (spec.pageMin != null && spec.pageMax != null) {
    return `${namePart}${spec.bookSpecUid} (${spec.pageMin}~${spec.pageMax})`;
  }
  return `${namePart}${spec.bookSpecUid}`;
};

const templateRequiresImage = (detail: any) => {
  const definitions = detail?.parameters?.definitions;
  if (!definitions || typeof definitions !== 'object') {
    return false;
  }

  return Object.values(definitions).some((definition: any) => {
    const binding = String(definition?.binding ?? definition?.type ?? '').toLowerCase();
    const requiresImageBinding = binding === 'file' || binding === 'gallery' || binding === 'rowgallery';
    return requiresImageBinding && definition?.required === true;
  });
};

const toTemplateOptions = (templates: TemplateSummary[]): TemplateOption[] => {
  return templates.map((template) => {
    const detail = templateDetailsByUid.value[template.templateUid];
    const requiresImage = templateRequiresImage(detail);
    const disabledReason = requiresImage && !hasAnyPhoto.value ? 'This template requires at least one attached photo.' : null;
    return {
      ...template,
      requiresImage,
      disabledReason,
    };
  });
};

const coverTemplateOptions = computed(() => toTemplateOptions(coverTemplates.value));
const contentTemplateOptions = computed(() => toTemplateOptions(contentTemplates.value));

const templateHelpText = (kind: 'cover' | 'content') => {
  const options = kind === 'cover' ? coverTemplateOptions.value : contentTemplateOptions.value;
  if (options.length === 0) {
    return 'No template options available.';
  }
  const unavailableCount = options.filter((option) => !!option.disabledReason).length;
  if (unavailableCount === 0) {
    return 'All templates are currently selectable.';
  }
  return `${unavailableCount} template(s) are unavailable because images are required.`;
};

const getSelectedOption = (kind: 'cover' | 'content') => {
  const options = kind === 'cover' ? coverTemplateOptions.value : contentTemplateOptions.value;
  const selectedUid = kind === 'cover' ? bookForm.coverTemplateUid : bookForm.contentTemplateUid;
  return options.find((option) => option.templateUid === selectedUid) || null;
};

const firstSelectableTemplateUid = (options: TemplateOption[]) => {
  return options.find((option) => !option.disabledReason)?.templateUid || '';
};

const ensureTemplateSelectionsValid = () => {
  const selectedCover = getSelectedOption('cover');
  if (!selectedCover || selectedCover.disabledReason) {
    bookForm.coverTemplateUid = firstSelectableTemplateUid(coverTemplateOptions.value);
  }

  const selectedContent = getSelectedOption('content');
  if (!selectedContent || selectedContent.disabledReason) {
    bookForm.contentTemplateUid = firstSelectableTemplateUid(contentTemplateOptions.value);
  }
};

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
    fetchError.value = err.message || 'Failed to load album data.';
  }
};

const loadBookSpecs = async () => {
  isBookSpecsLoading.value = true;
  configLoadError.value = null;
  try {
    const specs = (await albumApi.getBookSpecs()) as BookSpecSummary[];
    bookSpecs.value = specs;
    if (!bookForm.bookSpecUid && specs.length > 0) {
      bookForm.bookSpecUid = specs[0].bookSpecUid;
    }
  } catch (err: any) {
    configLoadError.value = err?.message || 'Failed to load book specs.';
  } finally {
    isBookSpecsLoading.value = false;
  }
};

const loadBookSpecDetail = async (bookSpecUid: string) => {
  if (!bookSpecUid) {
    selectedBookSpecDetail.value = null;
    return;
  }

  isBookSpecDetailLoading.value = true;
  try {
    const detail = (await albumApi.getBookSpecDetail(bookSpecUid)) as BookSpecDetail;
    selectedBookSpecDetail.value = detail;
  } catch (err: any) {
    selectedBookSpecDetail.value = null;
    configLoadError.value = err?.message || 'Failed to load selected book spec detail.';
  } finally {
    isBookSpecDetailLoading.value = false;
  }
};

const loadTemplateDetailCached = async (templateUid: string) => {
  if (!templateUid) return null;
  if (templateDetailsByUid.value[templateUid]) {
    return templateDetailsByUid.value[templateUid];
  }
  const detail = await albumApi.getTemplateDetail(templateUid);
  templateDetailsByUid.value[templateUid] = detail;
  return detail;
};

const loadTemplates = async (bookSpecUid: string) => {
  if (!bookSpecUid) {
    coverTemplates.value = [];
    contentTemplates.value = [];
    return;
  }

  isTemplatesLoading.value = true;
  configLoadError.value = null;
  try {
    const [covers, contents] = await Promise.all([
      albumApi.getTemplates(bookSpecUid, 'cover'),
      albumApi.getTemplates(bookSpecUid, 'content'),
    ]);

    coverTemplates.value = (covers as TemplateSummary[]).map((template) => ({
      templateUid: template.templateUid,
      name: template.name,
      title: template.title,
    }));

    contentTemplates.value = (contents as TemplateSummary[]).map((template) => ({
      templateUid: template.templateUid,
      name: template.name,
      title: template.title,
    }));

    const allTemplateUids = [...coverTemplates.value, ...contentTemplates.value].map((template) => template.templateUid);
    await Promise.all(
      allTemplateUids.map(async (uid) => {
        try {
          await loadTemplateDetailCached(uid);
        } catch {
          // TODO(sweetbook-api): confirm whether template detail may omit parameters/required metadata.
        }
      }),
    );

    ensureTemplateSelectionsValid();
  } catch (err: any) {
    coverTemplates.value = [];
    contentTemplates.value = [];
    configLoadError.value = err?.message || 'Failed to load templates for the selected spec.';
  } finally {
    isTemplatesLoading.value = false;
  }
};

const updateSelectedTemplateThumbnails = async () => {
  const loadThumbnail = async (templateUid: string) => {
    if (!templateUid) return null;
    const detail = await loadTemplateDetailCached(templateUid);
    return detail?.thumbnails?.layout ?? null;
  };

  try {
    coverThumbnail.value = await loadThumbnail(bookForm.coverTemplateUid);
  } catch {
    coverThumbnail.value = null;
  }

  try {
    contentThumbnail.value = await loadThumbnail(bookForm.contentTemplateUid);
  } catch {
    contentThumbnail.value = null;
  }
};

const onPhotoChange = (activityId: number, event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;

  const previousUrl = photoPreviewByActivity.value[activityId];
  if (previousUrl) {
    URL.revokeObjectURL(previousUrl);
  }

  photoFileByActivity.value[activityId] = file;
  photoPreviewByActivity.value[activityId] = URL.createObjectURL(file);
};

const clearPhoto = (activityId: number) => {
  const previousUrl = photoPreviewByActivity.value[activityId];
  if (previousUrl) {
    URL.revokeObjectURL(previousUrl);
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
    return 'Select at least one activity.';
  }
  if (!bookForm.title.trim()) return 'Enter a book title.';
  if (!bookForm.bookSpecUid) return 'Select a book spec.';
  if (!bookForm.coverTemplateUid) return 'Select a cover template.';
  if (!bookForm.contentTemplateUid) return 'Select a content template.';

  const selectedCover = getSelectedOption('cover');
  const selectedContent = getSelectedOption('content');
  if (selectedCover?.disabledReason) return selectedCover.disabledReason;
  if (selectedContent?.disabledReason) return selectedContent.disabledReason;

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
    previewError.value = err?.message || 'Failed to calculate estimate.';
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
    await albumStore.generateBook(albumStore.currentAlbum.albumId, {
      title: bookForm.title.trim(),
      bookSpecUid: bookForm.bookSpecUid,
      coverTemplateUid: bookForm.coverTemplateUid,
      contentTemplateUid: bookForm.contentTemplateUid,
    });
    router.push({ name: 'order-list', params: { albumId: albumStore.currentAlbum.albumId } });
  } catch (err: any) {
    generateError.value = err?.message || 'Failed to generate the book. Please try again.';
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
  async (value) => {
    showPreview.value = false;
    estimate.value = null;
    generateError.value = null;
    bookForm.coverTemplateUid = '';
    bookForm.contentTemplateUid = '';
    coverThumbnail.value = null;
    contentThumbnail.value = null;

    await loadBookSpecDetail(value);
    await loadTemplates(value);
    await updateSelectedTemplateThumbnails();
  },
);

watch(
  () => [bookForm.coverTemplateUid, bookForm.contentTemplateUid],
  async () => {
    showPreview.value = false;
    estimate.value = null;
    generateError.value = null;
    await updateSelectedTemplateThumbnails();
  },
);

watch(
  hasAnyPhoto,
  () => {
    showPreview.value = false;
    estimate.value = null;
    ensureTemplateSelectionsValid();
  },
  { deep: false },
);

onMounted(async () => {
  await loadAlbumData();
  await loadBookSpecs();
  if (bookForm.bookSpecUid) {
    await loadBookSpecDetail(bookForm.bookSpecUid);
    await loadTemplates(bookForm.bookSpecUid);
    await updateSelectedTemplateThumbnails();
  }
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

.field-help {
  color: var(--color-olive-gray);
  font-size: 0.75rem;
}

.state-stack {
  margin-top: 12px;
  display: grid;
  gap: 6px;
}

.state-message {
  color: var(--color-olive-gray);
  margin: 0;
}

.spec-detail {
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: var(--color-warm-sand);
}

.spec-detail h4 {
  margin: 0 0 8px;
}

.spec-detail p {
  margin: 4px 0;
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
