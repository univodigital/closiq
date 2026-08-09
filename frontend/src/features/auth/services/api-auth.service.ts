import { apiFetch } from "@/lib/api-client";
import { mapSellerProfile, type RawSellerProfile } from "@/lib/api-mappers";
import { setAccessToken } from "@/lib/auth-token";
import type { User } from "@/shared/types";
import type { AuthService, RegistrationProfile } from "./auth.service";

interface OtpInitiateResponse {
  otpSessionId: string;
  expiresInSeconds: number;
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
  displayName: string;
  avatarUrl?: string | null;
  roles: string[];
  createdAt: string;
  sellerProfile?: RawSellerProfile;
}

interface AuthTokenResponse {
  accessToken: string;
  expiresIn: number;
  tokenType: string;
  user: UserSummaryResponse;
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
    displayName: raw.displayName,
    avatarUrl: raw.avatarUrl ?? null,
    roles: raw.roles as User["roles"],
    sellerProfile: raw.sellerProfile ? mapSellerProfile(raw.sellerProfile) : undefined,
    createdAt: raw.createdAt,
  };
}

function storeAuthToken(data: AuthTokenResponse) {
  setAccessToken(data.accessToken);
}

export class ApiAuthService implements AuthService {
  async register(phone: string, acceptTerms = true) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify({ phone, acceptTerms }),
    });
    return { otpSessionId: data.otpSessionId, expiresInSeconds: data.expiresInSeconds };
  }

  async login(phone: string) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ phone }),
    });
    return { otpSessionId: data.otpSessionId, expiresInSeconds: data.expiresInSeconds };
  }

  async loginWithPassword(identifier: string, password: string) {
    const data = await apiFetch<AuthTokenResponse>("/auth/login-password", {
      method: "POST",
      body: JSON.stringify({ identifier, password }),
    });
    storeAuthToken(data);
  }

  async verifyOtp(otpSessionId: string, otp: string, profile?: RegistrationProfile) {
    const data = await apiFetch<AuthTokenResponse>("/auth/verify-otp", {
      method: "POST",
      body: JSON.stringify({
        otpSessionId,
        otp,
        purpose: profile ? "REGISTER" : "LOGIN",
        profile: profile
          ? {
              username: profile.username,
              password: profile.password,
              email: profile.email,
            }
          : undefined,
      }),
    });
    storeAuthToken(data);
  }

  async forgotPassword(phone: string) {
    const data = await apiFetch<OtpInitiateResponse>("/auth/forgot-password", {
      method: "POST",
      body: JSON.stringify({ phone }),
    });
    return { otpSessionId: data.otpSessionId, expiresInSeconds: data.expiresInSeconds };
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
