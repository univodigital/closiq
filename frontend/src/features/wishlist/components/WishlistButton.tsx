"use client";

import { Heart } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useWishlist } from "@/features/wishlist/hooks/useWishlist";
import { cn } from "@/lib/utils";

export function WishlistButton({
  productId,
  className,
  size = "lg",
}: {
  productId: string;
  className?: string;
  size?: "sm" | "md" | "lg";
}) {
  const { isWishlisted, toggleWishlist, isPending } = useWishlist();
  const saved = isWishlisted(productId);

  return (
    <Button
      type="button"
      variant="outline"
      size={size}
      className={cn("gap-2", className)}
      disabled={isPending}
      onClick={() => toggleWishlist(productId)}
      aria-pressed={saved}
    >
      <Heart className={cn("h-4 w-4", saved && "fill-destructive text-destructive")} />
      {saved ? "Saved to wishlist" : "Add to wishlist"}
    </Button>
  );
}
