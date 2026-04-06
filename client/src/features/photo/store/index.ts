import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { ActivityPhotoItemResponse } from '../types';
import * as photoApi from '../api/photoApi';

export const usePhotoStore = defineStore('photo', () => {
  // Activity ID -> Photo List mapping
  const photosByActivity = ref<Record<number, ActivityPhotoItemResponse[]>>({});
  const isLoading = ref(false);

  const fetchPhotos = async (albumId: number, activityId: number) => {
    const res = await photoApi.listPhotos(albumId, activityId);
    photosByActivity.value[activityId] = res;
  };

  const uploadPhoto = async (albumId: number, activityId: number, file: File) => {
    isLoading.value = true;
    try {
      await photoApi.uploadPhoto(albumId, activityId, file);
      await fetchPhotos(albumId, activityId);
    } finally {
      isLoading.value = false;
    }
  };

  const deletePhoto = async (albumId: number, activityId: number, photoId: number) => {
    await photoApi.deletePhoto(albumId, activityId, photoId);
    await fetchPhotos(albumId, activityId);
  };

  return {
    photosByActivity,
    isLoading,
    fetchPhotos,
    uploadPhoto,
    deletePhoto,
  };
});
