import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CategoryResponse, CreateCategoryRequest, PermissionEntryResponse,
  SetPermissionRequest, UpdateCategoryRequest
} from '../models/category.model';
import { PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly apiUrl = `${environment.apiUrl}/categories`;

  constructor(private http: HttpClient) {}

  listCategories(page = 0, size = 20): Observable<PageResponse<CategoryResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<CategoryResponse>>(this.apiUrl, { params });
  }

  getCategory(id: string): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.apiUrl}/${id}`);
  }

  createCategory(request: CreateCategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.apiUrl, request);
  }

  updateCategory(id: string, request: UpdateCategoryRequest): Observable<CategoryResponse> {
    return this.http.patch<CategoryResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteCategory(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getUserPermissions(categoryId: string): Observable<PermissionEntryResponse[]> {
    return this.http.get<PermissionEntryResponse[]>(`${this.apiUrl}/${categoryId}/permissions/users`);
  }

  getGroupPermissions(categoryId: string): Observable<PermissionEntryResponse[]> {
    return this.http.get<PermissionEntryResponse[]>(`${this.apiUrl}/${categoryId}/permissions/groups`);
  }

  setUserPermission(categoryId: string, userId: string, request: SetPermissionRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${categoryId}/permissions/users/${userId}`, request);
  }

  removeUserPermission(categoryId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${categoryId}/permissions/users/${userId}`);
  }

  setGroupPermission(categoryId: string, groupId: string, request: SetPermissionRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${categoryId}/permissions/groups/${groupId}`, request);
  }

  removeGroupPermission(categoryId: string, groupId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${categoryId}/permissions/groups/${groupId}`);
  }
}
