import { apiFetch } from "@/lib/api-client";
import type { BagItem } from "@/features/checkout/bag/bag-store";

export interface ServerCartItem {
  productSlug: string;
  variantSize: string;
  rentalStartDate: string;
  rentalEndDate: string;
}

function toServerItem(item: BagItem): ServerCartItem {
  return {
    productSlug: item.slug,
    variantSize: item.size,
    rentalStartDate: item.start,
    rentalEndDate: item.end,
  };
}

function fromServerItem(item: ServerCartItem): BagItem {
  return {
    slug: item.productSlug,
    size: item.variantSize,
    start: item.rentalStartDate,
    end: item.rentalEndDate,
  };
}

export async function fetchServerCart(): Promise<BagItem[]> {
  const raw = await apiFetch<{ items: ServerCartItem[] }>("/cart");
  return (raw.items ?? []).map(fromServerItem);
}

export async function replaceServerCart(items: BagItem[]): Promise<BagItem[]> {
  const raw = await apiFetch<{ items: ServerCartItem[] }>("/cart", {
    method: "PUT",
    body: JSON.stringify({ items: items.map(toServerItem) }),
  });
  return (raw.items ?? []).map(fromServerItem);
}

export async function mergeGuestCart(guestItems: BagItem[]): Promise<BagItem[]> {
  const raw = await apiFetch<{ items: ServerCartItem[] }>("/cart/merge", {
    method: "POST",
    body: JSON.stringify({ guestItems: guestItems.map(toServerItem) }),
  });
  return (raw.items ?? []).map(fromServerItem);
}
