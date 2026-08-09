"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Heart } from "lucide-react";
import { toast } from "sonner";
import { wishlistService } from "@/features/wishlist/services";
import { ProductCard } from "@/shared/components/display/ProductCard";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { ROUTES } from "@/shared/constants/routes";
import { useAuth } from "@/providers/AuthProvider";

export default function WishlistPage() {
  const router = useRouter();
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const qc = useQueryClient();

  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.replace(`${ROUTES.login}?returnUrl=${encodeURIComponent(ROUTES.wishlist)}`);
    }
  }, [authLoading, isAuthenticated, router]);

  const { data, isLoading } = useQuery({
    queryKey: ["wishlist"],
    queryFn: () => wishlistService.list(),
    enabled: isAuthenticated,
    retry: false,
  });

  const remove = useMutation({
    mutationFn: (id: string) => wishlistService.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["wishlist"] });
      toast.success("Removed from wishlist");
    },
  });

  const items = data?.data ?? [];

  if (authLoading || !isAuthenticated) {
    return (
      <Container embedded>
        <PageHeader title="Wishlist" description="Your saved pieces" />
        <div className="grid grid-cols-2 gap-5 md:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <ProductCardSkeleton key={i} />
          ))}
        </div>
      </Container>
    );
  }

  return (
    <Container embedded>
      <PageHeader title="Wishlist" description={`${items.length} saved pieces`} />
      {isLoading ? (
        <div className="grid grid-cols-2 gap-5 md:grid-cols-3 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => <ProductCardSkeleton key={i} />)}
        </div>
      ) : items.length === 0 ? (
        <EmptyState
          icon={Heart}
          title="Your wishlist is empty"
          description="Save pieces you love while browsing."
          actionLabel="Discover"
          actionHref={ROUTES.products}
        />
      ) : (
        <div className="grid grid-cols-2 gap-5 md:grid-cols-3 md:gap-7 lg:grid-cols-4">
          {items.map((p) => (
            <ProductCard
              key={p.id}
              product={p}
              isWishlisted
              onWishlistToggle={(id) => remove.mutate(id)}
            />
          ))}
        </div>
      )}
    </Container>
  );
}
