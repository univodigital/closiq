import type { User } from "@/shared/types";
import type { Gender } from "@/shared/types";

export interface RegistrationProfile {
  username: string;
  password: string;
  email: string;
  firstName: string;
  lastName: string;
  gender: Gender;
}

export interface OtpInitiateResult {
  otpSessionId: string;
  phone?: string;
  expiresInSeconds: number;
  resendAvailableInSeconds: number;
}

export interface VerifyOtpResult {
  existingAccount?: boolean;
  requiresProfile?: boolean;
  phone?: string;
  authenticated: boolean;
}

export interface AuthService {
  register(phone: string, acceptTerms?: boolean, email?: string): Promise<OtpInitiateResult>;
  login(identifier: string): Promise<OtpInitiateResult>;
  loginWithPassword(identifier: string, password: string): Promise<void>;
  verifyRegistrationOtp(otpSessionId: string, otp: string): Promise<VerifyOtpResult>;
  verifyLoginOtp(otpSessionId: string, otp: string): Promise<void>;
  completeRegistration(otpSessionId: string, profile: RegistrationProfile): Promise<void>;
  resendOtp(otpSessionId: string): Promise<OtpInitiateResult>;
  forgotPassword(identifier: string): Promise<OtpInitiateResult>;
  resetPassword(otpSessionId: string, otp: string, newPassword: string): Promise<void>;
  logout(): Promise<void>;
  getCurrentUser(): Promise<User | null>;
}
