import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateGroupRequest, GroupResponse, UpdateGroupRequest } from '../models/group.model';
import { PageResponse } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly apiUrl = `${environment.apiUrl}/groups`;

  constructor(private http: HttpClient) {}

  listGroups(page = 0, size = 10, search?: string): Observable<PageResponse<GroupResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get<PageResponse<GroupResponse>>(this.apiUrl, { params });
  }

  getGroup(id: string): Observable<GroupResponse> {
    return this.http.get<GroupResponse>(`${this.apiUrl}/${id}`);
  }

  createGroup(request: CreateGroupRequest): Observable<GroupResponse> {
    return this.http.post<GroupResponse>(this.apiUrl, request);
  }

  updateGroup(id: string, request: UpdateGroupRequest): Observable<GroupResponse> {
    return this.http.patch<GroupResponse>(`${this.apiUrl}/${id}`, request);
  }

  deleteGroup(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  addMember(groupId: string, userId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${groupId}/members/${userId}`, {});
  }

  removeMember(groupId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${groupId}/members/${userId}`);
  }
}
