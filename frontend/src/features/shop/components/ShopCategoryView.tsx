"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ShopProductGrid } from "@/features/shop/components/ShopProductGrid";
import {
  SHOP_AUDIENCE_LABELS,
  shopGarmentCategories,
  type ShopAudienceSlug,
} from "@/shared/constants/shop-nav";

export function ShopCategoryView({
  audience,
  slug,
}: {
  audience: ShopAudienceSlug;
  slug: string;
}) {
  const audienceLabel = SHOP_AUDIENCE_LABELS[audience];
  const categoryLabel =
    shopGarmentCategories(audience).find((c) => c.slug === slug)?.label ?? slug;

  return (
    <Container className="py-10 md:py-14">
      <PageHeader
        title={`${audienceLabel} · ${categoryLabel}`}
        description={`${categoryLabel} from our ${audienceLabel.toLowerCase()} collection.`}
        breadcrumb={`${audienceLabel} · Category`}
      />
      <ShopProductGrid params={{ audience, garmentType: slug, sort: "createdAt:desc" }} />
    </Container>
  );
}
