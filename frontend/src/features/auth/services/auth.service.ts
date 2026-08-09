import type { User } from "@/shared/types";

export interface AuthService {
  register(phone: string, acceptTerms?: boolean): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  login(phone: string): Promise<{ otpSessionId: string; expiresInSeconds: number }>;
  verifyOtp(
    otpSessionId: string,
    otp: string,
    profile?: { firstName: string; lastName: string; email?: string },
  ): Promise<void>;
  logout(): Promise<void>;
  getCurrentUser(): Promise<User | null>;
}
