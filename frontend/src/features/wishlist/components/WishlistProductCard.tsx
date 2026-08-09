"use client";

import { ProductCard } from "@/shared/components/display/ProductCard";
import { useWishlist } from "@/features/wishlist/hooks/useWishlist";
import type { Product } from "@/shared/types";

export function WishlistProductCard({
  product,
  className,
}: {
  product: Product;
  className?: string;
}) {
  const { isWishlisted, toggleWishlist } = useWishlist();

  return (
    <ProductCard
      product={product}
      className={className}
      isWishlisted={isWishlisted(product.id)}
      onWishlistToggle={toggleWishlist}
    />
  );
}
