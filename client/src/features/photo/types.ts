export interface ActivityPhotoUploadResponse {
  photoId: number;
  originalFileName: string;
  contentType: string;
  fileSize: number;
}

export interface ActivityPhotoItemResponse {
  photoId: number;
  originalFileName: string;
  contentType: string;
  fileSize: number;
  createdAt: string;
}

export interface ActivityPhotoDeleteResponse {
  deleted: boolean;
}
