import { Permission } from './auth.model';

export interface CategoryResponse {
  id: string;
  name: string;
  description: string;
  createdAt: string;
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
