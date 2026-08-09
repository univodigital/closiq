"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/providers/AuthProvider";
import { ROUTES } from "@/shared/constants/routes";

export function SellerGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isLoading, isAuthenticated, hasRole } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace(ROUTES.login);
      return;
    }
    if (!hasRole("SELLER")) {
      router.replace(ROUTES.account.becomeSeller);
    }
  }, [isLoading, isAuthenticated, hasRole, router]);

  if (isLoading) {
    return <p className="p-8 text-sm text-muted-foreground">Loading seller workspace…</p>;
  }

  if (!isAuthenticated || !hasRole("SELLER")) {
    return null;
  }

  return <>{children}</>;
}
