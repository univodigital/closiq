import { apiFetch } from "@/lib/api-client";
import { mapSellerProfile, type RawSellerProfile } from "@/lib/api-mappers";
import { setAccessToken } from "@/lib/auth-token";
import type { User } from "@/shared/types";
import type { Gender } from "@/shared/types";
import type {
  AuthService,
  OtpInitiateResult,
  RegistrationProfile,
  VerifyOtpResult,
} from "./auth.service";

interface OtpInitiateResponse {
  otpSessionId: string;
  phone?: string;
  expiresInSeconds: number;
  resendAvailableInSeconds: number;
}

interface UserSummaryResponse {
  id: string;
  phone: string;
  phoneVerified: boolean;
  alternatePhone?: string;
  email?: string;
  emailVerified?: boolean;
  alternateEmail?: string;
  username?: string;
  firstName: string;
  lastName: string;
  gender: Gender;
  displayName: string;
  avatarUrl?: string | null;
  roles: string[];
  createdAt: string;
  sellerProfile?: RawSellerProfile;
}

interface VerifyOtpResponse {
  existingAccount?: boolean;
  requiresProfile?: boolean;
  phone?: string;
  accessToken?: string;
  expiresIn?: number;
  tokenType?: string;
  user?: UserSummaryResponse;
  isNewUser?: boolean;
}

interface AuthTokenResponse {
  accessToken: string;
  expiresIn: number;
  tokenType: string;
  user: UserSummaryResponse;
  isNewUser?: boolean;
}

function mapUser(raw: UserSummaryResponse): User {
  return {
    id: raw.id,
    phone: raw.phone,
    phoneVerified: raw.phoneVerified,
    alternatePhone: raw.alternatePhone,
    email: raw.email,
    emailVerified: raw.emailVerified,
    alternateEmail: raw.alternateEmail,
    firstName: raw.firstName,
    lastName: raw.lastName,
    gender: raw.gender,
    displayName: raw.displayName,
    avatarUrl: raw.avatarUrl ?? null,
    roles: raw.roles as User["roles"],
    sellerProfile: raw.sellerProfile ? mapSellerProfile(raw.sellerProfile) : undefined,
    createdAt: raw.createdAt,
  };
}

function mapOtpInitiate(data: OtpInitiateResponse): OtpInitiateResult {
  return {
    otpSessionId: data.otpSessionId,
    phone: data.phone,
    expiresInSeconds: data.expiresInSeconds,
    resendAvailableInSeconds: data.resendAvailableInSeconds,
  };
}

function storeAuthToken(data: AuthTokenResponse) {
  setAccessToken(data.accessToken);
}

function mapVerifyResult(data: VerifyOtpResponse): VerifyOtpResult {
  if (data.accessToken) {
    setAccessToken(data.accessToken);
  }
  return {
    existingAccount: data.existingAccount,
    requiresProfile: data.requiresProfile,
    phone: data.phone,
    authenticated: Boolean(data.accessToken),
  };
}

export class ApiAuthService implements AuthService {
  async register(phone: string, acceptTerms = true, email?: string) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify({
        phone,
        acceptTerms,
        email: email?.trim().toLowerCase(),
      }),
    });
    return mapOtpInitiate(data);
  }

  async login(identifier: string) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ identifier }),
    });
    return mapOtpInitiate(data);
  }

  async loginWithPassword(identifier: string, password: string) {
    const data = await apiFetch<AuthTokenResponse>("/auth/login-password", {
      method: "POST",
      body: JSON.stringify({ identifier, password }),
    });
    storeAuthToken(data);
  }

  async verifyRegistrationOtp(otpSessionId: string, otp: string) {
    const data = await apiFetch<VerifyOtpResponse>("/auth/verify-otp", {
      method: "POST",
      body: JSON.stringify({
        otpSessionId,
        otp,
        purpose: "REGISTER",
      }),
    });
    return mapVerifyResult(data);
  }

  async verifyLoginOtp(otpSessionId: string, otp: string) {
    const data = await apiFetch<VerifyOtpResponse>("/auth/verify-otp", {
      method: "POST",
      body: JSON.stringify({
        otpSessionId,
        otp,
        purpose: "LOGIN",
      }),
    });
    if (!data.accessToken) {
      throw new Error("Authentication failed");
    }
    setAccessToken(data.accessToken);
  }

  async completeRegistration(otpSessionId: string, profile: RegistrationProfile) {
    const data = await apiFetch<AuthTokenResponse>("/auth/complete-registration", {
      method: "POST",
      body: JSON.stringify({
        otpSessionId,
        profile: {
          username: profile.username,
          password: profile.password,
          email: profile.email,
          firstName: profile.firstName,
          lastName: profile.lastName,
          gender: profile.gender,
        },
      }),
    });
    storeAuthToken(data);
  }

  async resendOtp(otpSessionId: string) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/resend-otp", {
      method: "POST",
      body: JSON.stringify({ otpSessionId }),
    });
    return mapOtpInitiate(data);
  }

  async forgotPassword(identifier: string) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ identifier }),
    });
    return mapOtpInitiate(data);
  }

  async resetPassword(otpSessionId: string, otp: string, newPassword: string) {
    const data = await apiFetch<AuthTokenResponse>("/auth/reset-password", {
      method: "POST",
      body: JSON.stringify({ otpSessionId, otp, newPassword }),
    });
    storeAuthToken(data);
  }

  async logout() {
    try {
      await apiFetch<void>("/auth/logout", { method: "POST", body: "{}", auth: false });
    } finally {
      setAccessToken(null);
    }
  }

  async getCurrentUser() {
    try {
      const data = await apiFetch<UserSummaryResponse>("/auth/me");
      return mapUser(data);
    } catch {
      setAccessToken(null);
      return null;
    }
  }
}

export const apiAuthService = new ApiAuthService();
