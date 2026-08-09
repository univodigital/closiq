export type ShopNavLink = {
  slug: string;
  label: string;
};

export type ShopAudienceSlug = "men" | "women" | "kids";

export const SHOP_AUDIENCE_SLUGS: ShopAudienceSlug[] = ["men", "women", "kids"];

export function isShopAudience(value: string): value is ShopAudienceSlug {
  return SHOP_AUDIENCE_SLUGS.includes(value as ShopAudienceSlug);
}

export const SHOP_AUDIENCE_LABELS: Record<ShopAudienceSlug, string> = {
  men: "Men's",
  women: "Women's",
  kids: "Kids'",
};

/** Garment / product-type categories shown under "Shop by category". */
export const SHOP_GARMENT_CATEGORIES: Record<ShopAudienceSlug, ShopNavLink[]> = {
  women: [
    { slug: "sarees", label: "Sarees" },
    { slug: "lehengas", label: "Lehengas" },
    { slug: "gowns", label: "Gowns" },
    { slug: "anarkalis", label: "Anarkalis" },
    { slug: "indo-western", label: "Indo-western" },
  ],
  men: [
    { slug: "suits", label: "Suits" },
    { slug: "sherwanis", label: "Sherwanis" },
    { slug: "kurtas", label: "Kurtas" },
    { slug: "bandhgala", label: "Bandhgala" },
    { slug: "indo-western", label: "Indo-western" },
  ],
  kids: [
    { slug: "lehengas", label: "Lehengas" },
    { slug: "kurtas", label: "Kurtas" },
    { slug: "ethnic-sets", label: "Ethnic sets" },
    { slug: "gowns", label: "Gowns" },
  ],
};

export const SHOP_OCCASIONS: ShopNavLink[] = [
  { slug: "wedding", label: "Wedding" },
  { slug: "festival", label: "Festival" },
  { slug: "office", label: "Office" },
  { slug: "party", label: "Party" },
];

/** Curated entry points — "Discover" column in the mega menu. */
export const SHOP_DISCOVER: ShopNavLink[] = [
  { slug: "new-in", label: "New In" },
  { slug: "trending", label: "Trending" },
  { slug: "all", label: "Shop all" },
];

export function shopGarmentCategories(audience: ShopAudienceSlug): ShopNavLink[] {
  return SHOP_GARMENT_CATEGORIES[audience] ?? [];
}
