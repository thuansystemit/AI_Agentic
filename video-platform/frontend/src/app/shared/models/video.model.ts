export interface VideoResponse {
  id: number;
  title: string;
  description: string;
  fileName: string;
  fileSize: number;
  contentType: string;
  streamUrl: string;
  owner: VideoOwner;
  createdAt: string;
  updatedAt: string;
}

export interface VideoOwner {
  id: number;
  username: string;
}

export interface VideoUpdateRequest {
  title?: string;
  description?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}
