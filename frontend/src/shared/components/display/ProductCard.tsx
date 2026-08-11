"use client";

import Link from "next/link";
import Image from "next/image";
import { Heart } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import type { Product } from "@/shared/types";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";
import { ProductListingAvailability } from "@/features/products/components/ProductListingAvailability";
import type { ListingDateAvailability } from "@/features/products/constants/listing-availability";

interface ProductCardProps {
  product: Product;
  onWishlistToggle?: (productId: string) => void;
  isWishlisted?: boolean;
  className?: string;
  onChangeDates?: () => void;
}

function resolveListingAvailability(product: Product): ListingDateAvailability | null {
  if (product.availableForDates == null) return null;
  return product.availableForDates ? "available" : "unavailable";
}

export function ProductCard({
  product,
  onWishlistToggle,
  isWishlisted,
  className,
  onChangeDates,
}: ProductCardProps) {
  const listingAvailability = resolveListingAvailability(product);

  return (
    <article className={cn("group", className)}>
      <Link href={ROUTES.product(product.slug)} className="block">
        <div className="relative aspect-[3/4] overflow-hidden rounded-sm bg-muted">
          <Image
            src={product.images[0]}
            alt={product.title}
            fill
            className={cn(
              "object-cover transition-transform duration-500 group-hover:scale-[1.02]",
              listingAvailability === "unavailable" && "opacity-80",
            )}
            sizes="(max-width: 640px) 50vw, 25vw"
          />
          {product.includesTrial && (
            <Badge variant="trial" className="absolute left-2 top-2">
              15-min trial
            </Badge>
          )}
          {onWishlistToggle && (
            <button
              type="button"
              onClick={(e) => {
                e.preventDefault();
                onWishlistToggle(product.id);
              }}
              className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-background/80 backdrop-blur-sm"
              aria-label={isWishlisted ? "Remove from wishlist" : "Add to wishlist"}
            >
              <Heart className={cn("h-4 w-4", isWishlisted && "fill-destructive text-destructive")} />
            </button>
          )}
        </div>
        <div className="mt-3 space-y-1">
          <p className="label-caps text-muted-foreground">{product.designer}</p>
          <h3 className="font-heading text-base leading-snug">{product.title}</h3>
          {listingAvailability && (
            <ProductListingAvailability status={listingAvailability} onChangeDates={onChangeDates} />
          )}
          <div className="flex items-center justify-between pt-1">
            <p className="font-mono text-sm text-foreground">
              {formatCurrency(product.pricePerDay)}
              <span className="text-muted-foreground">/day</span>
            </p>
            {product.rating > 0 && (
              <p className="text-xs text-muted-foreground">★ {product.rating}</p>
            )}
          </div>
        </div>
      </Link>
    </article>
  );
}
