import { notFound } from "next/navigation";
import { ShopAudienceView } from "@/features/shop/components/ShopAudienceView";
import { isShopAudience } from "@/shared/constants/shop-nav";

export default async function ShopAudiencePage({
  params,
}: {
  params: Promise<{ audience: string }>;
}) {
  const { audience } = await params;
  if (!isShopAudience(audience)) notFound();

  return <ShopAudienceView audience={audience} />;
}
