"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import type { User, UserRole } from "@/shared/types";
import { authService } from "@/features/auth/services";
import type { RegistrationProfile, VerifyOtpResult } from "@/features/auth/services/auth.service";
import { fetchUserAddresses, fetchUserProfile } from "@/features/user/services";
import { setSessionCookie } from "@/lib/session-cookie";

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  hasRole: (role: UserRole) => boolean;
  login: (identifier: string) => Promise<{ otpSessionId: string; resendAvailableInSeconds?: number }>;
  loginWithPassword: (identifier: string, password: string) => Promise<void>;
  verifyRegistrationOtp: (otpSessionId: string, otp: string) => Promise<VerifyOtpResult>;
  verifyLoginOtp: (otpSessionId: string, otp: string) => Promise<void>;
  completeRegistration: (otpSessionId: string, profile: RegistrationProfile) => Promise<void>;
  resetPassword: (otpSessionId: string, otp: string, newPassword: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshUser = useCallback(async () => {
    try {
      const me = await authService.getCurrentUser();
      if (!me) {
        setUser(null);
        setSessionCookie(false);
        return;
      }
      try {
        const [profile, addresses] = await Promise.all([
          fetchUserProfile(),
          fetchUserAddresses(),
        ]);
        setUser({
          ...profile,
          roles: me.roles,
          sellerProfile: profile.sellerProfile ?? me.sellerProfile,
          addresses,
        });
      } catch {
        setUser(me);
      }
      setSessionCookie(true);
    } catch {
      setUser(null);
      setSessionCookie(false);
    }
  }, []);

  useEffect(() => {
    refreshUser().finally(() => setIsLoading(false));
  }, [refreshUser]);

  useEffect(() => {
    const onSessionExpired = () => {
      setUser(null);
      setSessionCookie(false);
    };
    window.addEventListener("closiq:session-expired", onSessionExpired);
    return () => window.removeEventListener("closiq:session-expired", onSessionExpired);
  }, []);

  const login = useCallback(async (identifier: string) => {
    return authService.login(identifier);
  }, []);

  const loginWithPassword = useCallback(
    async (identifier: string, password: string) => {
      await authService.loginWithPassword(identifier, password);
      await refreshUser();
    },
    [refreshUser],
  );

  const verifyRegistrationOtp = useCallback(
    async (otpSessionId: string, otp: string) => {
      return authService.verifyRegistrationOtp(otpSessionId, otp);
    },
    [],
  );

  const verifyLoginOtp = useCallback(
    async (otpSessionId: string, otp: string) => {
      await authService.verifyLoginOtp(otpSessionId, otp);
      await refreshUser();
    },
    [refreshUser],
  );

  const completeRegistration = useCallback(
    async (otpSessionId: string, profile: RegistrationProfile) => {
      await authService.completeRegistration(otpSessionId, profile);
      await refreshUser();
    },
    [refreshUser],
  );

  const resetPassword = useCallback(
    async (otpSessionId: string, otp: string, newPassword: string) => {
      await authService.resetPassword(otpSessionId, otp, newPassword);
      await refreshUser();
    },
    [refreshUser],
  );

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // Server-side logout is best-effort; local session is always cleared.
    } finally {
      setUser(null);
      setSessionCookie(false);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      isAuthenticated: !!user,
      hasRole: (role) => user?.roles.includes(role) ?? false,
      login,
      loginWithPassword,
      verifyRegistrationOtp,
      verifyLoginOtp,
      completeRegistration,
      resetPassword,
      logout,
      refreshUser,
    }),
    [
      user,
      isLoading,
      login,
      loginWithPassword,
      verifyRegistrationOtp,
      verifyLoginOtp,
      completeRegistration,
      resetPassword,
      logout,
      refreshUser,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
