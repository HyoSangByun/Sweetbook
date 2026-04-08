<script setup lang="ts">
export type UploadedPhotoItem = { fileName: string; previewUrl: string };

const props = defineProps<{
  bookUid: string | null;
  isUploadingPhoto: boolean;
  coverFrontPreviewUrl: string | null;
  coverFrontFileName: string | null;
  selectedActivities: Array<{
    albumActivityId: number;
    activityDateTime: string;
    activityName: string;
    distanceKm: number;
  }>;
  activityPhotos: Record<number, UploadedPhotoItem[]>;
}>();

const emit = defineEmits<{
  (e: 'cover-file-change', event: Event): void;
  (e: 'activity-files-change', albumActivityId: number, event: Event): void;
}>();

const formatDate = (value: string) => {
  return new Date(value).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
};
</script>

<template>
  <section class="card">
    <h3 class="section-title">2단계: 사진 업로드</h3>

    <div class="field-grid">
      <label class="field">
        <span>표지 메인 사진 (필수)</span>
        <input type="file" accept="image/*" :disabled="!bookUid || isUploadingPhoto" @change="emit('cover-file-change', $event)" />
      </label>
    </div>

    <div class="thumb-row">
      <div class="thumb-col">
        <p>표지 메인</p>
        <img v-if="coverFrontPreviewUrl" :src="coverFrontPreviewUrl" class="thumb" alt="표지 메인" />
        <p v-if="coverFrontFileName" class="state-message">{{ coverFrontFileName }}</p>
      </div>
    </div>

    <div v-if="selectedActivities.length > 0" class="activity-list">
      <article v-for="activity in selectedActivities" :key="activity.albumActivityId" class="activity-item">
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
            :disabled="!bookUid || isUploadingPhoto"
            @change="emit('activity-files-change', activity.albumActivityId, $event)"
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
</template>

