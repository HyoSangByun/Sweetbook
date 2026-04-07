<template>
  <div class="album-detail-page">
    <header class="page-header">
      <div class="container header-content">
        <router-link to="/" class="btn-back">???쒕룞 紐⑸줉?쇰줈</router-link>
        <h1 class="logo">SweetBook</h1>
        <div class="user-actions">
          <button @click="handleLogout" class="btn-logout">濡쒓렇?꾩썐</button>
        </div>
      </div>
    </header>

    <main class="container" v-if="albumStore.currentAlbum && !fetchError">
      <!-- Album Info Header -->
      <section class="album-info-section">
        <div class="album-info-header">
          <div class="month-badge">{{ albumStore.currentAlbum.month }}</div>
          <h2 class="album-title">{{ albumStore.currentAlbum.title }}</h2>
          <p v-if="albumStore.currentAlbum.subtitle" class="album-subtitle">{{ albumStore.currentAlbum.subtitle }}</p>
        </div>
        
        <div class="album-review-card">
          <h3 class="card-label">Monthly Review</h3>
          <p class="review-content">{{ albumStore.currentAlbum.monthlyReview || '?묒꽦??由щ럭媛 ?놁뒿?덈떎.' }}</p>
          <button @click="isEditingInfo = true" class="btn-edit-info">?뺣낫 ?섏젙</button>
        </div>
      </section>

      <!-- Selected Activities & Photos -->
      <section class="activities-section">
        <h3 class="section-subtitle">?좏깮???쒕룞 ({{ albumStore.currentAlbum.selectedActivityCount }})</h3>
        
        <div v-if="albumStore.currentAlbum.selectedActivities.length === 0" class="empty-state">
          ?쒕룞 紐⑸줉?먯꽌 ?쒕룞???좏깮?댁＜?몄슂.
        </div>
        
        <div v-else class="activity-list">
          <div v-for="activity in albumStore.currentAlbum.selectedActivities" :key="activity.albumActivityId" class="activity-item-card">
            <div class="activity-main-info">
              <div class="activity-type-tag">{{ activity.activityType }}</div>
              <h4 class="activity-name">{{ activity.activityName }}</h4>
              <p class="activity-time">{{ formatDate(activity.activityDateTime) }}</p>
              <div class="activity-stats">
                <span>{{ activity.distanceKm.toFixed(2) }}km</span>
                <span>{{ formatDuration(activity.movingTimeSeconds) }}</span>
              </div>
            </div>

            <div class="photo-management">
              <h5 class="photo-label">異붽????ъ쭊</h5>
              <div class="photo-grid">
                <div v-for="photo in photoStore.photosByActivity[activity.activityId]" :key="photo.photoId" class="photo-item">
                  <img :src="`/api/albums/${albumStore.currentAlbum.albumId}/activities/${activity.activityId}/photos/${photo.photoId}`" :alt="photo.originalFileName" class="photo-img" />
                  <button
                    @click="handleDeletePhoto(activity.activityId, photo.photoId)"
                    class="btn-delete-photo"
                    :disabled="deleteLoadingByPhoto[photo.photoId]"
                  >
                    횞
                  </button>
                </div>
                <div class="upload-placeholder" @click="triggerPhotoUpload(activity.activityId)">
                  <span v-if="photoStore.isLoading">...</span>
                  <span v-else>+ 異붽?</span>
                </div>
              </div>
              <input 
                type="file" 
                :ref="el => setFileInputRef(el, activity.activityId)" 
                @change="(e) => handlePhotoUpload(e, activity.activityId)" 
                accept="image/*" 
                style="display: none" 
              />
            </div>

            <button
              @click="handleDeselect(activity.activityId)"
              class="btn-deselect"
              :disabled="deselectLoadingByActivity[activity.activityId]"
            >
              {{ deselectLoadingByActivity[activity.activityId] ? '泥섎━ 以?..' : '?댁젣' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Order Status Banner -->
      <section class="order-section">
        <div class="order-card">
          <div class="order-status-info">
            <span class="status-label">吏꾪뻾 ?곹깭</span>
            <span class="status-value">{{ albumStore.currentAlbum.status }}</span>
          </div>
          <button v-if="albumStore.currentAlbum.status === 'DRAFT'" class="btn-order" @click="goToOrderList">二쇰Ц?섍린</button>
        </div>
      </section>
    </main>

    <div v-else-if="fetchError" class="error-state">
      <p class="error-message">{{ fetchError }}</p>
      <button @click="loadAlbumData" class="btn-retry">?ㅼ떆 ?쒕룄</button>
    </div>

    <div v-else-if="albumStore.isLoading" class="loading-state">
      ?⑤쾾 ?뺣낫瑜?遺덈윭?ㅻ뒗 以?..
    </div>

    <!-- Info Edit Modal (Placeholder) -->
    <div v-if="isEditingInfo" class="modal-overlay" @click.self="closeInfoModal">
      <div class="modal-content">
        <button class="btn-modal-close" @click="closeInfoModal" aria-label="?뺣낫 ?섏젙 紐⑤떖 ?リ린">?リ린</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../../auth/store';
import { useAlbumStore } from '../store';
import { usePhotoStore } from '../../photo/store';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const albumStore = useAlbumStore();
const photoStore = usePhotoStore();

const isEditingInfo = ref(false);
const fetchError = ref<string | null>(null);
const fileInputRefs = ref<Record<number, HTMLInputElement | null>>({});
const deleteLoadingByPhoto = ref<Record<number, boolean>>({});
const deselectLoadingByActivity = ref<Record<number, boolean>>({});
const deselectError = ref<string | null>(null);

const closeInfoModal = () => {
  isEditingInfo.value = false;
};

const handleEscClose = (event: KeyboardEvent) => {
  if (event.key === 'Escape' && isEditingInfo.value) {
    closeInfoModal();
  }
};

onMounted(async () => {
  window.addEventListener('keydown', handleEscClose);
  await loadAlbumData();
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEscClose);
});

const loadAlbumData = async () => {
  const albumId = Number(route.params.id);
  if (!albumId) return;

  fetchError.value = null;
  try {
    await albumStore.fetchAlbum(albumId);
    if (albumStore.currentAlbum) {
      // Fetch photos for each activity
      for (const activity of albumStore.currentAlbum.selectedActivities) {
        await photoStore.fetchPhotos(albumId, activity.activityId);
      }
    }
  } catch (err: any) {
    fetchError.value = err.message || '?⑤쾾 ?뺣낫瑜?遺덈윭?ㅻ뒗 ???ㅽ뙣?덉뒿?덈떎.';
  }
};

const setFileInputRef = (el: any, activityId: number) => {
  if (el) fileInputRefs.value[activityId] = el as HTMLInputElement;
};

const triggerPhotoUpload = (activityId: number) => {
  fileInputRefs.value[activityId]?.click();
};

const handlePhotoUpload = async (event: Event, activityId: number) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0 && albumStore.currentAlbum) {
    try {
      await photoStore.uploadPhoto(albumStore.currentAlbum.albumId, activityId, target.files[0]);
    } catch (err: any) {
      alert('?낅줈???ㅽ뙣: ' + (err.message || '?????녿뒗 ?먮윭'));
    } finally {
      target.value = ''; // Reset file input
    }
  }
};

const handleDeletePhoto = async (activityId: number, photoId: number) => {
  if (!albumStore.currentAlbum) return;
  if (deleteLoadingByPhoto.value[photoId]) return;
  if (!confirm('???ъ쭊????젣?섏떆寃좎뒿?덇퉴?')) return;

  deleteLoadingByPhoto.value[photoId] = true;
  try {
    await photoStore.deletePhoto(albumStore.currentAlbum.albumId, activityId, photoId);
  } catch (err: any) {
    alert('??젣 ?ㅽ뙣: ' + (err.message || '?????녿뒗 ?먮윭'));
  } finally {
    deleteLoadingByPhoto.value[photoId] = false;
  }
};

const handleDeselect = async (activityId: number) => {
  if (!albumStore.currentAlbum) return;
  if (deselectLoadingByActivity.value[activityId]) return;

  const albumId = albumStore.currentAlbum.albumId;
  deselectError.value = null;
  deselectLoadingByActivity.value[activityId] = true;

  try {
    const res = await albumStore.deselectActivity(albumId, activityId);

    if (albumStore.currentAlbum) {
      albumStore.currentAlbum.selectedActivities = albumStore.currentAlbum.selectedActivities.filter(
        (activity) => activity.activityId !== activityId,
      );
      albumStore.currentAlbum.selectedActivityCount = res.selectedActivityCount;
    }
  } catch (err: any) {
    deselectError.value = err?.message || '?쒕룞 ?댁젣 以??ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.';
    console.error('Failed to deselect activity:', err);
    alert(deselectError.value);
  } finally {
    deselectLoadingByActivity.value[activityId] = false;
  }
};

const goToOrderList = () => {
  if (!albumStore.currentAlbum) return;
  router.push({ name: 'order-list', params: { albumId: albumStore.currentAlbum.albumId } });
};

const handleLogout = () => {
  authStore.logout();
  router.push({ name: 'login' });
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
};

const formatDuration = (seconds: number) => {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}시간 ${m}분` : `${m}분`;
};
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
  margin-bottom: 40px;
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

.album-info-section {
  text-align: center;
  margin-bottom: 64px;
}

.month-badge {
  display: inline-block;
  padding: 4px 12px;
  background-color: var(--color-warm-sand);
  border-radius: 20px;
  font-size: 0.875rem;
  color: var(--color-olive-gray);
  margin-bottom: 16px;
}

.album-title {
  font-size: 3.5rem;
  margin-bottom: 12px;
}

.album-subtitle {
  font-family: var(--font-serif);
  font-size: 1.25rem;
  color: var(--color-olive-gray);
  margin-bottom: 32px;
}

.album-review-card {
  max-width: 800px;
  margin: 0 auto;
  background-color: var(--color-ivory);
  padding: 32px;
  border-radius: 12px;
  border: 1px solid var(--color-border-cream);
  position: relative;
}

.card-label {
  font-family: var(--font-serif);
  font-size: 1rem;
  color: var(--color-stone-gray);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.review-content {
  font-family: var(--font-serif);
  font-size: 1.125rem;
  line-height: 1.8;
  color: var(--color-near-black);
  margin-bottom: 24px;
}

.btn-edit-info {
  background-color: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 6px 12px;
  font-size: 0.75rem;
  border-radius: 6px;
}

.section-subtitle {
  font-size: 1.75rem;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--color-border-warm);
  padding-bottom: 12px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.activity-item-card {
  background-color: var(--color-white);
  padding: 32px;
  border-radius: 12px;
  border: 1px solid var(--color-border-cream);
  display: grid;
  grid-template-columns: 300px 1fr 100px;
  gap: 32px;
  align-items: start;
}

.activity-type-tag {
  display: inline-block;
  font-size: 0.625rem;
  font-weight: 500;
  padding: 2px 6px;
  background-color: var(--color-warm-sand);
  border-radius: 4px;
  color: var(--color-olive-gray);
  margin-bottom: 12px;
}

.activity-name {
  font-size: 1.25rem;
  margin-bottom: 8px;
}

.activity-time {
  font-size: 0.875rem;
  color: var(--color-stone-gray);
  margin-bottom: 12px;
}

.activity-stats {
  display: flex;
  gap: 16px;
  font-size: 0.875rem;
  font-weight: 500;
}

.photo-management {
  border-left: 1px solid var(--color-border-cream);
  padding-left: 32px;
}

.photo-label {
  font-size: 0.875rem;
  color: var(--color-stone-gray);
  margin-bottom: 16px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: 12px;
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  overflow: hidden;
  border-radius: 8px;
  background-color: var(--color-parchment);
}

.photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.btn-delete-photo {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 22px;
  height: 22px;
  border-radius: 999px;
  background-color: var(--color-white);
  color: var(--color-error);
  font-size: 0.875rem;
  line-height: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.btn-delete-photo:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.upload-placeholder {
  aspect-ratio: 1;
  background-color: var(--color-parchment);
  border: 1px dashed var(--color-border-warm);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 0.75rem;
  color: var(--color-olive-gray);
}

.btn-deselect {
  background-color: transparent;
  color: var(--color-error);
  font-size: 0.875rem;
  padding: 8px;
}

.order-section {
  margin-top: 80px;
  margin-bottom: 80px;
}

.order-card {
  background-color: var(--color-dark-surface);
  color: var(--color-white);
  padding: 40px;
  border-radius: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-label {
  display: block;
  font-size: 0.875rem;
  color: var(--color-warm-silver);
  margin-bottom: 8px;
}

.status-value {
  font-size: 1.5rem;
  font-weight: 500;
}

.btn-order {
  background-color: var(--color-terracotta);
  color: var(--color-white);
  padding: 16px 32px;
  font-size: 1.125rem;
  font-weight: 500;
  border-radius: 12px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  min-width: 320px;
  background: var(--color-white);
  border-radius: 12px;
  padding: 24px;
}

.btn-modal-close {
  background-color: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 12px;
  border-radius: 8px;
}

.error-state {
  text-align: center;
  padding: 100px 24px;
}

.error-message {
  color: var(--color-error);
  margin-bottom: 16px;
}

.btn-retry {
  background-color: var(--color-white);
  color: var(--color-error);
  border: 1px solid var(--color-error);
  padding: 8px 14px;
  border-radius: 8px;
}

.loading-state, .empty-state {
  text-align: center;
  padding: 100px;
  color: var(--color-stone-gray);
}
</style>

