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

export const getBookSpecs = () =>
  client.get<any[]>('/albums/book-specs');

export const getTemplates = (bookSpecUid: string, templateKind: 'cover' | 'content') =>
  client.get<any[]>(`/albums/templates?bookSpecUid=${encodeURIComponent(bookSpecUid)}&templateKind=${templateKind}`);

export const getTemplateDetail = (templateUid: string) =>
  client.get<any>(`/albums/templates/${templateUid}`);

export const estimateBookOrder = (
  albumId: number,
  payload: { title: string; bookSpecUid: string; coverTemplateUid: string; contentTemplateUid: string }
) =>
  client.post<{
    estimatedPageCount: number;
    productAmount: number | null;
    shippingFee: number | null;
    packagingFee: number | null;
    totalAmount: number | null;
    currency: string;
  }>(`/albums/${albumId}/book/estimate`, payload);

export const generateBook = (albumId: number) =>
  client.post<any>(`/albums/${albumId}/book`);
