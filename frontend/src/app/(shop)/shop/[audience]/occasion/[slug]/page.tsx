import { notFound } from "next/navigation";
import { ShopOccasionView } from "@/features/shop/components/ShopOccasionView";
import { isShopAudience, SHOP_OCCASIONS } from "@/shared/constants/shop-nav";

export default async function ShopOccasionPage({
  params,
}: {
  params: Promise<{ audience: string; slug: string }>;
}) {
  const { audience, slug } = await params;
  if (!isShopAudience(audience)) notFound();
  if (!SHOP_OCCASIONS.some((o) => o.slug === slug)) notFound();

  return <ShopOccasionView audience={audience} slug={slug} />;
}
