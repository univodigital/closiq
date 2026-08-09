"use client";

import { useQuery } from "@tanstack/react-query";
import { productService } from "@/features/products/services";
import { WishlistProductCard } from "@/features/wishlist/components/WishlistProductCard";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { Package } from "lucide-react";
import { ROUTES } from "@/shared/constants/routes";
import type { ProductListParams } from "@/shared/types";

export function ShopProductGrid({ params }: { params: ProductListParams }) {
  const products = useQuery({
    queryKey: ["shop-products", params],
    queryFn: () => productService.listProducts(params),
  });

  const items = products.data?.data ?? [];

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
        <WishlistProductCard key={p.id} product={p} />
      ))}
    </div>
  );
}
