export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  user: UserInfo;
}

export interface UserInfo {
  id: number;
  username: string;
  email: string;
}
