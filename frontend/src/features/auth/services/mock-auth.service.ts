import userProfile from "@/mocks/data/user-profile.json";
import { delay } from "@/mocks/utils/delay";
import type { AuthService, RegistrationProfile } from "./auth.service";
import type { User } from "@/shared/types";

const MOCK_OTP = "123456";
const MOCK_PASSWORD = "Password1";
let sessionActive = false;
let currentUser: User | null = null;

function mapUser(raw: typeof userProfile): User {
  return {
    ...raw,
    roles: raw.roles.map((r) => r.toUpperCase() as User["roles"][number]),
    sellerProfile: raw.sellerProfile
      ? {
          ...raw.sellerProfile,
          verificationStatus: raw.sellerProfile.verificationStatus.toUpperCase() as "VERIFIED",
        }
      : undefined,
  };
}

export class MockAuthService implements AuthService {
  async register(phone: string, _acceptTerms = true) {
    await delay(600);
    return { otpSessionId: `otp_${phone}`, expiresInSeconds: 300 };
  }

  async login(phone: string) {
    await delay(600);
    return { otpSessionId: `otp_${phone}`, expiresInSeconds: 300 };
  }

  async loginWithPassword(_identifier: string, password: string) {
    await delay(400);
    if (password !== MOCK_PASSWORD) {
      throw new Error("Invalid phone/username or password");
    }
    sessionActive = true;
    currentUser = mapUser(userProfile);
  }

  async verifyOtp(_otpSessionId: string, otp: string, profile?: RegistrationProfile) {
    await delay(400);
    if (otp !== MOCK_OTP) {
      throw new Error("Invalid OTP. Use 123456 for mock login.");
    }
    sessionActive = true;
    const base = mapUser(userProfile);
    currentUser = profile
      ? {
          ...base,
          firstName: profile.username,
          lastName: "",
          displayName: profile.username,
          roles: ["CUSTOMER"],
          sellerProfile: undefined,
        }
      : base;
  }

  async forgotPassword(phone: string) {
    await delay(600);
    return { otpSessionId: `reset_${phone}`, expiresInSeconds: 300 };
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
  }

  async getCurrentUser() {
    await delay(150);
    if (!sessionActive) return null;
    return currentUser ?? mapUser(userProfile);
  }
}

export const mockAuthService = new MockAuthService();
