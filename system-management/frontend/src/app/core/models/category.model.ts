import { Permission } from './auth.model';

export interface CategoryResponse {
  id: string;
  name: string;
  description: string;
  createdAt: string;
  effectivePermission: Permission | null;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
}

export interface UpdateCategoryRequest {
  name?: string;
  description?: string;
}

export interface SetPermissionRequest {
  permission: Permission;
}

export interface PermissionEntryResponse {
  subjectId: string;
  subjectName: string;
  permission: Permission;
}
