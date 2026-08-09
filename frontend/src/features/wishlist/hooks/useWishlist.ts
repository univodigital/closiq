"use client";

import { useMemo } from "react";
import { usePathname, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { ApiError } from "@/lib/api-client";
import { wishlistService } from "@/features/wishlist/services";
import { useAuth } from "@/providers/AuthProvider";
import { ROUTES } from "@/shared/constants/routes";

export function useWishlist() {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const qc = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["wishlist"],
    queryFn: () => wishlistService.list(),
    enabled: isAuthenticated,
  });

  const wishlistedIds = useMemo(
    () => new Set(data?.data.map((p) => p.id) ?? []),
    [data],
  );

  const add = useMutation({
    mutationFn: (productId: string) => wishlistService.add(productId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["wishlist"] });
      toast.success("Added to wishlist");
    },
    onError: (err: unknown) => {
      if (err instanceof ApiError && err.status === 403) {
        router.push(`${ROUTES.login}?returnUrl=${encodeURIComponent(pathname)}`);
        toast.error("Sign in to save pieces to your wishlist");
        return;
      }
      toast.error("Could not add to wishlist");
    },
  });

  const remove = useMutation({
    mutationFn: (productId: string) => wishlistService.remove(productId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["wishlist"] });
      toast.success("Removed from wishlist");
    },
    onError: () => toast.error("Could not remove from wishlist"),
  });

  function requireAuth() {
    router.push(`${ROUTES.login}?returnUrl=${encodeURIComponent(pathname)}`);
  }

  function isWishlisted(productId: string) {
    return wishlistedIds.has(productId);
  }

  function toggleWishlist(productId: string) {
    if (!isAuthenticated) {
      requireAuth();
      return;
    }
    if (wishlistedIds.has(productId)) {
      remove.mutate(productId);
    } else {
      add.mutate(productId);
    }
  }

  return {
    isLoading: isAuthenticated && isLoading,
    count: wishlistedIds.size,
    isWishlisted,
    toggleWishlist,
    isPending: add.isPending || remove.isPending,
  };
}
