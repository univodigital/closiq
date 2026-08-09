import { notFound } from "next/navigation";
import { ShopCategoryView } from "@/features/shop/components/ShopCategoryView";
import { isShopAudience, shopGarmentCategories } from "@/shared/constants/shop-nav";

export default async function ShopCategoryPage({
  params,
}: {
  params: Promise<{ audience: string; slug: string }>;
}) {
  const { audience, slug } = await params;
  if (!isShopAudience(audience)) notFound();

  const valid = shopGarmentCategories(audience).some((c) => c.slug === slug);
  if (!valid) notFound();

  return <ShopCategoryView audience={audience} slug={slug} />;
}
