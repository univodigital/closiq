import userProfile from "@/mocks/data/user-profile.json";
import { delay } from "@/mocks/utils/delay";
import type { AuthService, OtpInitiateResult, RegistrationProfile, VerifyOtpResult } from "./auth.service";
import type { User, Gender } from "@/shared/types";

const MOCK_OTP = "123456";
const MOCK_PASSWORD = "Password1";
let sessionActive = false;
let currentUser: User | null = null;
let registrationVerified = false;

function mapUser(raw: typeof userProfile): User {
  return {
    ...raw,
    gender: raw.gender as Gender,
    roles: raw.roles.map((r) => r.toUpperCase() as User["roles"][number]),
    sellerProfile: raw.sellerProfile
      ? {
          ...raw.sellerProfile,
          verificationStatus: raw.sellerProfile.verificationStatus.toUpperCase() as "VERIFIED",
        }
      : undefined,
  };
}

function otpResult(otpSessionId: string, phone?: string): OtpInitiateResult {
  return {
    otpSessionId,
    phone,
    expiresInSeconds: 300,
    resendAvailableInSeconds: 60,
  };
}

export class MockAuthService implements AuthService {
  async register(phone: string, _acceptTerms = true, _email?: string) {
    await delay(600);
    registrationVerified = false;
    return otpResult(`otp_${phone}`, phone);
  }

  async login(phone: string) {
    await delay(600);
    return otpResult(`otp_${phone}`, phone);
  }

  async loginWithPassword(_identifier: string, password: string) {
    await delay(400);
    if (password !== MOCK_PASSWORD) {
      throw new Error("Invalid phone/username or password");
    }
    sessionActive = true;
    currentUser = mapUser(userProfile);
  }

  async verifyRegistrationOtp(_otpSessionId: string, otp: string): Promise<VerifyOtpResult> {
    await delay(400);
    if (otp !== MOCK_OTP) {
      throw new Error("Invalid OTP. Use 123456 for mock login.");
    }
    if (_otpSessionId.includes("existing")) {
      return { existingAccount: true, phone: "+919876543210", authenticated: false };
    }
    registrationVerified = true;
    return { requiresProfile: true, phone: "+919876543210", authenticated: false };
  }

  async verifyLoginOtp(_otpSessionId: string, otp: string) {
    await delay(400);
    if (otp !== MOCK_OTP) {
      throw new Error("Invalid OTP. Use 123456 for mock login.");
    }
    sessionActive = true;
    currentUser = mapUser(userProfile);
  }

  async completeRegistration(_otpSessionId: string, profile: RegistrationProfile) {
    await delay(400);
    if (!registrationVerified) {
      throw new Error("Verify OTP before completing registration");
    }
    sessionActive = true;
    const base = mapUser(userProfile);
    currentUser = {
      ...base,
      firstName: profile.firstName,
      lastName: profile.lastName,
      gender: profile.gender,
      displayName: `${profile.firstName} ${profile.lastName.charAt(0)}.`,
      roles: ["CUSTOMER"],
      sellerProfile: undefined,
    };
  }

  async resendOtp(otpSessionId: string) {
    await delay(300);
    return otpResult(otpSessionId);
  }

  async forgotPassword(phone: string) {
    await delay(600);
    return otpResult(`reset_${phone}`);
  }

  async resetPassword(_otpSessionId: string, otp: string, newPassword: string) {
    await delay(400);
    if (otp !== MOCK_OTP) {
      throw new Error("Invalid OTP. Use 123456 for mock reset.");
    }
    if (newPassword.length < 8) {
      throw new Error("Password too short");
    }
    sessionActive = true;
    currentUser = mapUser(userProfile);
  }

  async logout() {
    await delay(200);
    sessionActive = false;
    currentUser = null;
    registrationVerified = false;
  }

  async getCurrentUser() {
    await delay(150);
    if (!sessionActive) return null;
    return currentUser ?? mapUser(userProfile);
  }
}

export const mockAuthService = new MockAuthService();
