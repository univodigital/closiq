"use client";

import { useQuery } from "@tanstack/react-query";
import { productService } from "@/features/products/services";
import { useListingParams } from "@/features/products/hooks/useListingParams";
import { WishlistProductCard } from "@/features/wishlist/components/WishlistProductCard";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { Package } from "lucide-react";
import { ROUTES } from "@/shared/constants/routes";
import type { ProductListParams } from "@/shared/types";

const LISTING_STALE_MS = 60_000;

export function ShopProductGrid({ params }: { params: ProductListParams }) {
  const listingParams = useListingParams(params);

  const products = useQuery({
    queryKey: ["shop-products", listingParams],
    queryFn: () => productService.listProducts(listingParams),
    staleTime: LISTING_STALE_MS,
    refetchOnWindowFocus: true,
  });

  const items = products.data?.data ?? [];

  function scrollToDateBar() {
    document.getElementById("listing-date-bar")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  if (products.isLoading) {
    return (
      <div className="grid grid-cols-2 gap-5 md:grid-cols-3 lg:grid-cols-4 md:gap-7">
        {Array.from({ length: 8 }).map((_, i) => (
          <ProductCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  if (!items.length) {
    return (
      <EmptyState
        icon={Package}
        title="No pieces here yet"
        description="Check back soon or browse another category."
        actionLabel="Browse all products"
        actionHref={ROUTES.products}
      />
    );
  }

  return (
    <div className="grid grid-cols-2 gap-5 md:grid-cols-3 lg:grid-cols-4 md:gap-7">
      {items.map((p) => (
        <WishlistProductCard key={p.id} product={p} onChangeDates={scrollToDateBar} />
      ))}
    </div>
  );
}
