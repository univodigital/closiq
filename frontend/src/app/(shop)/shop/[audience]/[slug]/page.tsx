import { notFound, redirect } from "next/navigation";
import { ShopDiscoverView } from "@/features/shop/components/ShopDiscoverView";
import { isShopAudience, SHOP_OCCASIONS } from "@/shared/constants/shop-nav";
import { ROUTES } from "@/shared/constants/routes";

const DISCOVER_SLUGS = new Set(["new-in", "trending"]);

export default async function ShopDiscoverPage({
  params,
}: {
  params: Promise<{ audience: string; slug: string }>;
}) {
  const { audience, slug } = await params;
  if (!isShopAudience(audience)) notFound();

  if (SHOP_OCCASIONS.some((o) => o.slug === slug)) {
    redirect(ROUTES.shop.occasion(audience, slug));
  }

  if (!DISCOVER_SLUGS.has(slug)) notFound();

  return <ShopDiscoverView audience={audience} slug={slug as "new-in" | "trending"} />;
}
