"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/providers/AuthProvider";
import { ROUTES } from "@/shared/constants/routes";
import { Skeleton } from "@/components/ui/skeleton";

export function AdminGuard({ children }: { children: React.ReactNode }) {
  const { isLoading, isAuthenticated, hasRole } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) {
      router.replace(`${ROUTES.login}?returnUrl=${encodeURIComponent(ROUTES.admin.dashboard)}`);
      return;
    }
    if (!hasRole("ADMIN")) {
      router.replace(ROUTES.home);
    }
  }, [isLoading, isAuthenticated, hasRole, router]);

  if (isLoading) {
    return <Skeleton className="mx-auto mt-20 h-64 w-full max-w-4xl" />;
  }

  if (!isAuthenticated || !hasRole("ADMIN")) {
    return null;
  }

  return <>{children}</>;
}
