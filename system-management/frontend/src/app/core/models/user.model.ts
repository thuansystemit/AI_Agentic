import { GlobalRole } from './auth.model';

export interface UserResponse {
  id: string;
  email: string;
  fullName: string;
  globalRole: GlobalRole;
  isActive: boolean;
  isLocked: boolean;
  createdAt: string;
}

export interface CreateUserRequest {
  email: string;
  fullName: string;
  password: string;
  globalRole: GlobalRole;
}

export interface UpdateUserRequest {
  fullName?: string;
  globalRole?: GlobalRole;
  isActive?: boolean;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
