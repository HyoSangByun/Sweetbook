import client from '../../../shared/api/client';
import type { 
  AlbumResponse, 
  CreateAlbumRequest, 
  UpdateAlbumRequest, 
  SelectAlbumActivitiesResponse, 
  DeselectAlbumActivityResponse 
} from '../types';

export const createAlbum = (data: CreateAlbumRequest) => 
  client.post<AlbumResponse>('/albums', data);

export const getAlbum = (albumId: number) => 
  client.get<AlbumResponse>(`/albums/${albumId}`);

export const updateAlbum = (albumId: number, data: UpdateAlbumRequest) => 
  client.patch<AlbumResponse>(`/albums/${albumId}`, data);

export const selectActivities = (albumId: number, activityIds: number[]) => 
  client.post<SelectAlbumActivitiesResponse>(`/albums/${albumId}/activities`, { activityIds });

export const deselectActivity = (albumId: number, activityId: number) => 
  client.delete<DeselectAlbumActivityResponse>(`/albums/${albumId}/activities/${activityId}`);

export const createBookDraft = (albumId: number, payload: { title: string }) =>
  client.post<{ albumId: number; bookUid: string }>(`/albums/${albumId}/book/draft`, payload);

export const uploadBookPhoto = (albumId: number, file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return client.post<{ fileName: string }>(`/albums/${albumId}/book/photos`, formData);
};

export const applyBookCover = (
  albumId: number,
  payload: { coverPhotoFileName: string; subtitle: string }
) => client.post<void>(`/albums/${albumId}/book/cover`, payload);

export const addBookContents = (
  albumId: number,
  payload: { pages: Array<{ albumActivityId: number; photoFileNames: string[] }> }
) => client.post<void>(`/albums/${albumId}/book/contents`, payload);

export const finalizeBook = (albumId: number) =>
  client.post<{ albumId: number; bookUid: string; bookStatus: string; finalizedAt: string }>(`/albums/${albumId}/book/finalization`);

export const getAlbumBooks = (albumId: number) =>
  client.get<any[]>(`/albums/${albumId}/books`);
