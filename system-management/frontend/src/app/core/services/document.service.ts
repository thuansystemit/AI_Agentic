import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DocumentResponse, UpdateDocumentRequest } from '../models/document.model';
import { PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly apiUrl = `${environment.apiUrl}/documents`;

  constructor(private http: HttpClient) {}

  listDocuments(categoryId: string, page = 0, size = 10, search?: string): Observable<PageResponse<DocumentResponse>> {
    let params = new HttpParams().set('categoryId', categoryId).set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get<PageResponse<DocumentResponse>>(this.apiUrl, { params });
  }

  getDocument(id: string): Observable<DocumentResponse> {
    return this.http.get<DocumentResponse>(`${this.apiUrl}/${id}`);
  }

  uploadDocument(formData: FormData): Observable<DocumentResponse> {
    return this.http.post<DocumentResponse>(`${this.apiUrl}/upload`, formData);
  }

  updateDocument(id: string, request: UpdateDocumentRequest): Observable<DocumentResponse> {
    return this.http.patch<DocumentResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
