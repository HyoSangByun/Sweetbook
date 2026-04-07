<template>
  <div class="dashboard-page">
    <header class="page-header">
      <div class="container header-content">
        <h1 class="logo">SweetBook</h1>
        <div class="user-actions">
          <span v-if="authStore.me">{{ authStore.me.email }}</span>
          <button @click="goToCredits" class="btn-credits">Credits</button>
          <button @click="handleLogout" class="btn-logout">로그아웃</button>
        </div>
      </div>
    </header>

    <main class="container">
      <section class="activity-section">
        <div class="section-header">
          <h2 class="section-title">활동</h2>
          <div class="filter-actions">
            <span v-if="isMonthSelectorLoading" class="month-state-text">불러오는 중...</span>
            <div v-else-if="activityLoadError" class="month-error-wrap">
              <span class="month-state-text">불러오기에 실패했습니다.</span>
              <button type="button" class="btn-retry" @click="retryLoad">Retry</button>
            </div>
            <span v-else-if="!hasActivityData" class="month-state-text">활동 데이터 없음</span>
            <select v-else v-model="selectedMonth" @change="handleMonthChange" class="month-select">
              <option v-for="month in activityStore.months" :key="month" :value="month">
                {{ month }}
              </option>
            </select>
            <input type="file" ref="fileInput" @change="handleImport" accept=".csv" style="display: none" />
            <button @click="triggerImport" class="btn-import">CSV 가져오기</button>
          </div>
        </div>

        <div v-if="activityStore.stats" class="stats-banner">
          <div class="stats-item">
            <span class="stats-label">전체 활동</span>
            <span class="stats-value">{{ activityStore.stats.totalCount }}건</span>
          </div>
          <div class="stats-item">
            <span class="stats-label">총 거리</span>
            <span class="stats-value">{{ activityStore.stats.totalDistanceKm.toFixed(1) }}km</span>
          </div>
          <div class="stats-item">
            <span class="stats-label">총 시간</span>
            <span class="stats-value">{{ formatDuration(activityStore.stats.totalMovingTimeSeconds) }}</span>
          </div>
        </div>

        <div v-if="activityStore.isLoading" class="loading-state">
          불러오는 중...
        </div>
        <div v-else-if="activityLoadError" class="empty-state">
          데이터를 불러오지 못했습니다. 상단 Retry 버튼으로 다시 시도해 주세요.
        </div>
        <div v-else-if="activityStore.activities.length === 0" class="empty-state">
          이 달의 활동이 없습니다. CSV 파일을 가져와 보세요.
        </div>
        <div v-else class="activity-grid-container">
          <div class="selection-banner" v-if="selectedActivityIds.length > 0">
            <span>{{ selectedActivityIds.length }}개의 활동 선택됨</span>
            <button @click="handleCreateAlbum" class="btn-create-album" :disabled="albumStore.isLoading">
              {{ albumStore.isLoading ? '생성 중...' : '선택 활동으로 앨범 만들기' }}
            </button>
          </div>

          <div class="activity-grid">
            <div
              v-for="activity in activityStore.activities"
              :key="activity.activityId"
              class="activity-card"
              :class="{ 'is-selected': selectedActivityIds.includes(activity.activityId) }"
              @click="toggleSelection(activity.activityId)"
            >
              <div class="card-selection-indicator">
                <div class="checkbox" :class="{ checked: selectedActivityIds.includes(activity.activityId) }"></div>
              </div>
              <div class="activity-type">{{ activity.activityType }}</div>
              <h3 class="activity-name">{{ activity.activityName }}</h3>
              <div class="activity-meta">
                <span>{{ formatDate(activity.activityDateTime) }}</span>
                <span>{{ activity.distanceKm.toFixed(2) }}km</span>
                <span>{{ formatDuration(activity.movingTimeSeconds) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useActivityStore } from '../store';
import { useAlbumStore } from '../../album/store';
import { useAuthStore } from '../../auth/store';

const router = useRouter();
const authStore = useAuthStore();
const activityStore = useActivityStore();
const albumStore = useAlbumStore();

const selectedMonth = ref('');
const selectedActivityIds = ref<number[]>([]);
const fileInput = ref<HTMLInputElement | null>(null);
const hasActivityData = computed(() => activityStore.months.length > 0);
const activityLoadError = ref<string | null>(null);
const isMonthSelectorLoading = ref(false);

onMounted(async () => {
  await retryLoad();
});

const loadActivityData = async (month: string) => {
  selectedActivityIds.value = [];
  try {
    await Promise.all([
      activityStore.fetchActivities(month),
      activityStore.fetchStats(month),
    ]);
    activityLoadError.value = null;
  } catch (err: any) {
    activityLoadError.value = err?.message || '활동 데이터를 불러오지 못했습니다.';
    throw err;
  }
};

const retryLoad = async () => {
  isMonthSelectorLoading.value = true;
  try {
    await activityStore.fetchMonths();
    activityLoadError.value = null;

    if (activityStore.months.length > 0) {
      if (!selectedMonth.value || !activityStore.months.includes(selectedMonth.value)) {
        selectedMonth.value = activityStore.months[0];
      }
      await loadActivityData(selectedMonth.value);
    }
  } catch (err: any) {
    activityLoadError.value = err?.message || '목록을 불러오지 못했습니다.';
  } finally {
    isMonthSelectorLoading.value = false;
  }
};

const handleMonthChange = async () => {
  if (selectedMonth.value) {
    try {
      await loadActivityData(selectedMonth.value);
    } catch {
      // 에러 상태만 표시하고 선택 값은 유지
    }
  }
};

const toggleSelection = (id: number) => {
  const index = selectedActivityIds.value.indexOf(id);
  if (index > -1) {
    selectedActivityIds.value.splice(index, 1);
  } else {
    selectedActivityIds.value.push(id);
  }
};

const handleCreateAlbum = async () => {
  if (selectedActivityIds.value.length === 0) return;

  try {
    const album = await albumStore.createAlbum({
      month: selectedMonth.value,
      title: `${selectedMonth.value} 활동 기록`,
    });

    await albumStore.selectActivities(album.albumId, selectedActivityIds.value);
    router.push({ name: 'album-detail', params: { id: album.albumId } });
  } catch (err: any) {
    alert('앨범 생성 실패: ' + (err.message || '알 수 없는 오류'));
  }
};

const triggerImport = () => {
  fileInput.value?.click();
};

const handleImport = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files.length > 0) {
    try {
      await activityStore.importActivities(target.files[0]);
      alert('가져오기가 완료되었습니다.');
      await retryLoad();
    } catch (err: any) {
      alert('가져오기 실패: ' + (err.message || '알 수 없는 오류'));
    } finally {
      target.value = '';
    }
  }
};

const handleLogout = () => {
  authStore.logout();
  router.push({ name: 'login' });
};

const goToCredits = () => {
  router.push({ name: 'credit' });
};

const formatDate = (dateStr: string) => {
  const date = new Date(dateStr);
  return date.toLocaleDateString('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

const formatDuration = (seconds: number) => {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return h > 0 ? `${h}시간 ${m}분` : `${m}분`;
};
</script>

<style scoped>
.dashboard-page {
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

.logo {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  margin: 0;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 0.875rem;
  color: var(--color-olive-gray);
}

.btn-logout {
  background: none;
  color: var(--color-terracotta);
  font-size: 0.875rem;
  padding: 4px 8px;
  font-weight: 500;
}

.btn-credits {
  background-color: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  font-size: 0.875rem;
  padding: 6px 10px;
  box-shadow: 0 0 0 1px var(--color-ring-warm);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.section-title {
  font-size: 2.5rem;
  margin: 0;
}

.filter-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.month-state-text {
  font-size: 0.875rem;
  color: var(--color-olive-gray);
}

.month-error-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-retry {
  background-color: var(--color-white);
  color: var(--color-charcoal-warm);
  padding: 6px 10px;
  box-shadow: 0 0 0 1px var(--color-ring-warm);
}

.month-select {
  padding: 8px 16px;
  border-radius: 8px;
  border: 1px solid var(--color-border-warm);
  background-color: var(--color-white);
  font-family: var(--font-sans);
}

.btn-import {
  background-color: var(--color-warm-sand);
  color: var(--color-charcoal-warm);
  padding: 8px 16px;
  font-weight: 500;
  box-shadow: 0 0 0 1px var(--color-ring-warm);
}

.stats-banner {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-bottom: 40px;
}

.stats-item {
  background-color: var(--color-ivory);
  padding: 24px;
  border-radius: 12px;
  border: 1px solid var(--color-border-cream);
  text-align: center;
}

.stats-label {
  display: block;
  font-size: 0.875rem;
  color: var(--color-olive-gray);
  margin-bottom: 8px;
}

.stats-value {
  font-family: var(--font-serif);
  font-size: 1.5rem;
  color: var(--color-near-black);
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 24px;
}

.activity-grid-container {
  position: relative;
}

.selection-banner {
  position: sticky;
  top: 20px;
  z-index: 10;
  background-color: var(--color-near-black);
  color: var(--color-white);
  padding: 16px 24px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  box-shadow: rgba(0, 0, 0, 0.2) 0px 8px 24px;
}

.btn-create-album {
  background-color: var(--color-terracotta);
  color: var(--color-white);
  padding: 8px 16px;
  font-weight: 500;
  font-size: 0.875rem;
}

.activity-card {
  background-color: var(--color-white);
  padding: 24px;
  border-radius: 12px;
  border: 1px solid var(--color-border-cream);
  transition: all 0.2s ease;
  position: relative;
  cursor: pointer;
}

.activity-card.is-selected {
  border-color: var(--color-terracotta);
  background-color: var(--color-ivory);
}

.card-selection-indicator {
  position: absolute;
  top: 16px;
  right: 16px;
}

.checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid var(--color-border-warm);
  border-radius: 4px;
  transition: all 0.2s ease;
}

.checkbox.checked {
  background-color: var(--color-terracotta);
  border-color: var(--color-terracotta);
}

.checkbox.checked::after {
  content: '✓';
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 14px;
}

.activity-card:hover {
  transform: translateY(-4px);
  box-shadow: rgba(0, 0, 0, 0.05) 0px 4px 24px;
}

.activity-type {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 2px 8px;
  background-color: var(--color-warm-sand);
  border-radius: 4px;
  margin-bottom: 12px;
  color: var(--color-olive-gray);
}

.activity-name {
  font-size: 1.25rem;
  margin-bottom: 16px;
}

.activity-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.875rem;
  color: var(--color-stone-gray);
}

.loading-state,
.empty-state {
  text-align: center;
  padding: 80px;
  background-color: var(--color-ivory);
  border-radius: 16px;
  color: var(--color-stone-gray);
  border: 1px dashed var(--color-border-warm);
}
</style>
