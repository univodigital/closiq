"use client";

import { ProductCard } from "@/shared/components/display/ProductCard";
import { useWishlist } from "@/features/wishlist/hooks/useWishlist";
import type { Product } from "@/shared/types";

export function WishlistProductCard({
  product,
  className,
  onChangeDates,
}: {
  product: Product;
  className?: string;
  onChangeDates?: () => void;
}) {
  const { isWishlisted, toggleWishlist } = useWishlist();

  return (
    <ProductCard
      product={product}
      className={className}
      isWishlisted={isWishlisted(product.id)}
      onWishlistToggle={toggleWishlist}
      onChangeDates={onChangeDates}
    />
  );
}
