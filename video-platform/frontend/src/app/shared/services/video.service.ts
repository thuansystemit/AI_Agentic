import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PageResponse, VideoResponse, VideoUpdateRequest } from '../models/video.model';

@Injectable({
  providedIn: 'root',
})
export class VideoService {
  private readonly apiUrl = `${environment.apiUrl}/videos`;

  constructor(private http: HttpClient) {}

  listVideos(page: number = 0, size: number = 12, search?: string): Observable<PageResponse<VideoResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<PageResponse<VideoResponse>>(this.apiUrl, { params });
  }

  getVideo(id: number): Observable<VideoResponse> {
    return this.http.get<VideoResponse>(`${this.apiUrl}/${id}`);
  }

  uploadVideo(file: File, title: string, description?: string): Observable<VideoResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    if (description) {
      formData.append('description', description);
    }
    return this.http.post<VideoResponse>(this.apiUrl, formData);
  }

  updateVideo(id: number, request: VideoUpdateRequest): Observable<VideoResponse> {
    return this.http.put<VideoResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteVideo(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getMyVideos(page: number = 0, size: number = 12): Observable<PageResponse<VideoResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<VideoResponse>>(`${environment.apiUrl}/users/me/videos`, { params });
  }

  getStreamUrl(id: number): string {
    return `${environment.apiUrl}/videos/${id}/stream`;
  }
}
