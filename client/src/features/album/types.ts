export type AlbumProjectStatus = 'DRAFT' | 'READY' | 'ORDERED';
export type BookGenerationStatus = 'NOT_REQUESTED' | 'PENDING' | 'COMPLETED' | 'FAILED';

export interface AlbumActivityItemResponse {
  albumActivityId: number;
  activityId: number;
  externalActivityId: string;
  activityDateTime: string;
  activityType: string;
  activityName: string;
  distanceKm: number;
  movingTimeSeconds: number;
  memo: string | null;
}

export interface AlbumResponse {
  albumId: number;
  month: string;
  title: string;
  subtitle: string | null;
  monthlyReview: string | null;
  status: AlbumProjectStatus;
  bookUid: string | null;
  bookStatus: BookGenerationStatus;
  bookGeneratedAt: string | null;
  hasPhoto: boolean;
  selectedActivityCount: number;
  selectedActivities: AlbumActivityItemResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateAlbumRequest {
  month: string;
  title: string;
  subtitle?: string;
  monthlyReview?: string;
}

export interface UpdateAlbumRequest {
  title?: string;
  subtitle?: string;
  monthlyReview?: string;
}

export interface SelectAlbumActivitiesResponse {
  addedCount: number;
  skippedCount: number;
  selectedActivityCount: number;
}

export interface DeselectAlbumActivityResponse {
  deleted: boolean;
  selectedActivityCount: number;
}
