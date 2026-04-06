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
