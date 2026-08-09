import type { User } from "@/shared/types";

export interface RegistrationProfile {
  username: string;
  password: string;
  email?: string;
}

export interface AuthService {
  register(phone: string, acceptTerms?: boolean): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  login(phone: string): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  loginWithPassword(identifier: string, password: string): Promise<void>;
  verifyOtp(otpSessionId: string, otp: string, profile?: RegistrationProfile): Promise<void>;
  forgotPassword(phone: string): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  resetPassword(otpSessionId: string, otp: string, newPassword: string): Promise<void>;
  logout(): Promise<void>;
  getCurrentUser(): Promise<User | null>;
}
