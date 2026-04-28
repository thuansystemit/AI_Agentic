export type GlobalRole = 'ADMIN' | 'EDITOR' | 'VIEWER';
export type Permission = 'READ' | 'WRITE' | 'EDIT';

export interface AuthResponse {
  userId: string;
  email: string;
  fullName: string;
  globalRole: GlobalRole;
  // accessToken is delivered via HttpOnly cookie — never returned in the JSON body
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CurrentUser {
  userId: string;
  email: string;
  fullName: string;
  globalRole: GlobalRole;
}
