import type { User } from "@/shared/types";
import type { Gender } from "@/shared/types";

export interface RegistrationProfile {
  username: string;
  password: string;
  email?: string;
  firstName: string;
  lastName: string;
  gender: Gender;
}

export interface AuthService {
  register(phone: string, acceptTerms?: boolean, email?: string): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  login(identifier: string): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  loginWithPassword(identifier: string, password: string): Promise<void>;
  verifyOtp(otpSessionId: string, otp: string, profile?: RegistrationProfile): Promise<void>;
  forgotPassword(identifier: string): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  resetPassword(otpSessionId: string, otp: string, newPassword: string): Promise<void>;
  logout(): Promise<void>;
  getCurrentUser(): Promise<User | null>;
}
