import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { AlbumResponse, CreateAlbumRequest, UpdateAlbumRequest } from '../types';
import * as albumApi from '../api/albumApi';

export const useAlbumStore = defineStore('album', () => {
  const currentAlbum = ref<AlbumResponse | null>(null);
  const isLoading = ref(false);
  const isGeneratingBook = ref(false);

  const selectedActivityCount = computed(() => currentAlbum.value?.selectedActivityCount ?? 0);
  const hasPhoto = computed(() => currentAlbum.value?.hasPhoto ?? false);

  const fetchAlbum = async (id: number) => {
    isLoading.value = true;
    try {
      const res = await albumApi.getAlbum(id);
      currentAlbum.value = res;
    } finally {
      isLoading.value = false;
    }
  };

  const createAlbum = async (data: CreateAlbumRequest) => {
    const res = await albumApi.createAlbum(data);
    currentAlbum.value = res;
    return res;
  };

  const updateAlbum = async (id: number, data: UpdateAlbumRequest) => {
    const res = await albumApi.updateAlbum(id, data);
    currentAlbum.value = res;
    return res;
  };

  const selectActivities = async (albumId: number, activityIds: number[]) => {
    const res = await albumApi.selectActivities(albumId, activityIds);
    if (currentAlbum.value) {
      currentAlbum.value.selectedActivityCount = res.selectedActivityCount;
    }
    return res;
  };

  const deselectActivity = async (albumId: number, activityId: number) => {
    const res = await albumApi.deselectActivity(albumId, activityId);
    if (currentAlbum.value) {
      currentAlbum.value.selectedActivityCount = res.selectedActivityCount;
    }
    return res;
  };

  const createBookDraft = async (albumId: number, payload: { title: string }) => {
    return albumApi.createBookDraft(albumId, payload);
  };

  const finalizeBook = async (albumId: number) => {
    if (isGeneratingBook.value) {
      return null;
    }
    isGeneratingBook.value = true;
    try {
      const res = await albumApi.finalizeBook(albumId);
      await fetchAlbum(albumId);
      return res;
    } finally {
      isGeneratingBook.value = false;
    }
  };

  return {
    currentAlbum,
    isLoading,
    isGeneratingBook,
    selectedActivityCount,
    hasPhoto,
    fetchAlbum,
    createAlbum,
    updateAlbum,
    selectActivities,
    deselectActivity,
    createBookDraft,
    finalizeBook,
  };
});
