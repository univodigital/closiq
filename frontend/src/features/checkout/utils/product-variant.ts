import type { Product, ProductVariant } from "@/shared/types";

export function findVariantBySize(product: Product | undefined, size: string): ProductVariant | undefined {
  if (!product?.variants?.length || !size) return undefined;
  return product.variants.find((v) => v.size === size);
}

export function defaultAvailableSize(product: Product | undefined): string {
  if (!product?.variants?.length) return "";
  return product.variants.find((v) => v.available)?.size ?? product.variants[0]?.size ?? "";
}
