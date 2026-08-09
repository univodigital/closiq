"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { sellerService } from "@/features/seller/services";
import type { SellerBusinessProfile } from "@/features/seller/types";

interface SellerContextValue {
  profile: SellerBusinessProfile | null;
  isLoading: boolean;
  refreshProfile: () => Promise<void>;
}

const SellerContext = createContext<SellerContextValue | null>(null);

export function SellerProvider({ children }: { children: React.ReactNode }) {
  const [profile, setProfile] = useState<SellerBusinessProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const refreshProfile = useCallback(async () => {
    try {
      const data = await sellerService.getProfile();
      setProfile(data);
    } catch {
      setProfile(null);
    }
  }, []);

  useEffect(() => {
    refreshProfile().finally(() => setIsLoading(false));
  }, [refreshProfile]);

  const value = useMemo(
    () => ({
      profile,
      isLoading,
      refreshProfile,
    }),
    [profile, isLoading, refreshProfile],
  );

  return <SellerContext.Provider value={value}>{children}</SellerContext.Provider>;
}

export function useSeller() {
  const ctx = useContext(SellerContext);
  if (!ctx) throw new Error("useSeller must be used within SellerProvider");
  return ctx;
}
