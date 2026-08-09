import {
  SHOP_DISCOVER,
  SHOP_OCCASIONS,
  shopGarmentCategories,
  type ShopAudienceSlug,
  type ShopNavLink,
} from "@/shared/constants/shop-nav";
import { ROUTES } from "@/shared/constants/routes";

export type ShopLandingCard = ShopNavLink & {
  href: string;
  image: string;
  caption?: string;
};

/** Editorial imagery for occasion cards — shared across audiences. */
const OCCASION_IMAGES: Record<string, string> = {
  wedding:
    "https://images.unsplash.com/photo-1724857250888-15b17e3aaaa9?auto=format&fit=crop&w=900&q=80",
  festival:
    "https://plus.unsplash.com/premium_photo-1729038879276-f74f40fb8d21?auto=format&fit=crop&w=900&q=80",
  office:
    "https://images.unsplash.com/photo-1668620866239-9bcb044bdb8e?auto=format&fit=crop&w=900&q=80",
  party:
    "https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&w=900&q=80",
};

const DISCOVER_IMAGES: Record<string, string> = {
  "new-in":
    "https://images.unsplash.com/photo-1469334031218-e382a71b716b?auto=format&fit=crop&w=1200&q=80",
  trending:
    "https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80",
};

const DISCOVER_CAPTIONS: Record<string, string> = {
  "new-in": "Fresh drops this week",
  trending: "What everyone’s renting",
};

/** Garment imagery keyed by audience → category slug. */
const CATEGORY_IMAGES: Record<ShopAudienceSlug, Record<string, string>> = {
  men: {
    suits:
      "https://images.unsplash.com/photo-1594938298603-c8148c4dae35?auto=format&fit=crop&w=900&q=80",
    sherwanis:
      "https://images.unsplash.com/photo-1617127365659-c47fa864d8bc?auto=format&fit=crop&w=900&q=80",
    kurtas:
      "https://images.unsplash.com/photo-1507679799987-c73779587ccf?auto=format&fit=crop&w=900&q=80",
    bandhgala:
      "https://images.unsplash.com/photo-1617137968427-85924c800a22?auto=format&fit=crop&w=900&q=80",
    "indo-western":
      "https://images.unsplash.com/photo-1487222477894-8943e31ef7b2?auto=format&fit=crop&w=900&q=80",
  },
  women: {
    sarees:
      "https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&w=900&q=80",
    lehengas:
      "https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&w=900&q=80",
    gowns:
      "https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&w=900&q=80",
    anarkalis:
      "https://images.unsplash.com/photo-1595777457583-95e059d581b8?auto=format&fit=crop&w=900&q=80",
    "indo-western":
      "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=900&q=80",
  },
  kids: {
    lehengas:
      "https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?auto=format&fit=crop&w=900&q=80",
    kurtas:
      "https://images.unsplash.com/photo-1544717305-2782549b5136?auto=format&fit=crop&w=900&q=80",
    "ethnic-sets":
      "https://images.unsplash.com/photo-1503454537195-1dcabb73ffb9?auto=format&fit=crop&w=900&q=80",
    gowns:
      "https://images.unsplash.com/photo-1471286174890-9c112ffca5b4?auto=format&fit=crop&w=900&q=80",
  },
};

const FALLBACK_IMAGE =
  "https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=900&q=80";

export function shopLandingOccasions(audience: ShopAudienceSlug): ShopLandingCard[] {
  return SHOP_OCCASIONS.map((item) => ({
    ...item,
    href: ROUTES.shop.occasion(audience, item.slug),
    image: OCCASION_IMAGES[item.slug] ?? FALLBACK_IMAGE,
  }));
}

export function shopLandingCategories(audience: ShopAudienceSlug): ShopLandingCard[] {
  const images = CATEGORY_IMAGES[audience];
  return shopGarmentCategories(audience).map((item) => ({
    ...item,
    href: ROUTES.shop.category(audience, item.slug),
    image: images[item.slug] ?? FALLBACK_IMAGE,
  }));
}

/** Discover cards for landing — excludes self-link “all”. */
export function shopLandingDiscover(audience: ShopAudienceSlug): ShopLandingCard[] {
  return SHOP_DISCOVER.filter((item) => item.slug !== "all").map((item) => ({
    ...item,
    href: ROUTES.shop.discover(audience, item.slug),
    image: DISCOVER_IMAGES[item.slug] ?? FALLBACK_IMAGE,
    caption: DISCOVER_CAPTIONS[item.slug],
  }));
}
