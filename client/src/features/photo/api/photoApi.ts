import client from '../../../shared/api/client';
import type { 
  ActivityPhotoUploadResponse, 
  ActivityPhotoItemResponse, 
  ActivityPhotoDeleteResponse 
} from '../types';

export const uploadPhoto = (albumId: number, activityId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return client.post<ActivityPhotoUploadResponse>(
    `/albums/${albumId}/activities/${activityId}/photos`,
    formData
  );
};

export const listPhotos = (albumId: number, activityId: number) => 
  client.get<ActivityPhotoItemResponse[]>(
    `/albums/${albumId}/activities/${activityId}/photos`
  );

export const deletePhoto = (albumId: number, activityId: number, photoId: number) => 
  client.delete<ActivityPhotoDeleteResponse>(
    `/albums/${albumId}/activities/${activityId}/photos/${photoId}`
  );
