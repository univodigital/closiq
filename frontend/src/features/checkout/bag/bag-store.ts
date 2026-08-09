import { ROUTES } from "@/shared/constants/routes";

export interface BagItem {
  /** Stable line id — same product can appear once; upsert matches on slug. */
  slug: string;
  size: string;
  start: string;
  end: string;
}

export const BAG_STORAGE_KEY = "closiq_bag";

function normalizeItem(raw: Record<string, unknown>): BagItem | null {
  const slug =
    typeof raw.slug === "string"
      ? raw.slug
      : typeof raw.productId === "string"
        ? "" // legacy id-only rows can't be restored without a slug
        : "";
  const size = typeof raw.size === "string" ? raw.size : "";
  const start = typeof raw.start === "string" ? raw.start : "";
  const end = typeof raw.end === "string" ? raw.end : "";
  if (!slug) return null;
  return { slug, size, start, end };
}

export function readBagItemsFromStorage(): BagItem[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = localStorage.getItem(BAG_STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed
      .map((item) => (item && typeof item === "object" ? normalizeItem(item as Record<string, unknown>) : null))
      .filter((item): item is BagItem => !!item);
  } catch {
    return [];
  }
}

export function writeBagItemsToStorage(items: BagItem[]) {
  if (typeof window === "undefined") return;
  localStorage.setItem(BAG_STORAGE_KEY, JSON.stringify(items));
}

export function isCompleteBagItem(item: BagItem): boolean {
  return !!(item.slug && item.size && item.start && item.end);
}

export function upsertBagItems(items: BagItem[], item: BagItem): BagItem[] {
  const next = [...items];
  const index = next.findIndex((i) => i.slug === item.slug);
  if (index >= 0) next[index] = item;
  else next.push(item);
  return next;
}

export function getBagHref(): string {
  return ROUTES.checkout.bag;
}
