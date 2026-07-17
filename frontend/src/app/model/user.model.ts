export type UserRole = 'USER' | 'ADMIN';

export interface UserAccount {
  id: string;
  fullName: string;
  email: string;
  role: UserRole;
  enabled: boolean;
  createdAt?: string;
}

export interface AuthResponse {
  token: string;
  user: UserAccount;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  fullName: string;
}
