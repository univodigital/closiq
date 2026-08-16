"use client";

import { useCallback } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/providers/AuthProvider";
import { ROUTES } from "@/shared/constants/routes";
import { markLoggedOutToastPending } from "@/features/auth/lib/logout-session";

export function useLogoutAction() {
  const queryClient = useQueryClient();
  const { logout } = useAuth();

  return useCallback(async () => {
    await logout();
    queryClient.removeQueries({ queryKey: ["wishlist"] });
    markLoggedOutToastPending();
    window.location.assign(ROUTES.home);
  }, [logout, queryClient]);
}
