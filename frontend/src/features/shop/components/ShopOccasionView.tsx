"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ShopProductGrid } from "@/features/shop/components/ShopProductGrid";
import { SHOP_AUDIENCE_LABELS, SHOP_OCCASIONS, type ShopAudienceSlug } from "@/shared/constants/shop-nav";

export function ShopOccasionView({
  audience,
  slug,
}: {
  audience: ShopAudienceSlug;
  slug: string;
}) {
  const audienceLabel = SHOP_AUDIENCE_LABELS[audience];
  const occasionLabel = SHOP_OCCASIONS.find((o) => o.slug === slug)?.label ?? slug;

  return (
    <Container className="py-10 md:py-14">
      <PageHeader
        title={`${audienceLabel} · ${occasionLabel}`}
        description={`${occasionLabel} pieces from our ${audienceLabel.toLowerCase()} edit.`}
        breadcrumb={`${audienceLabel} · Occasion`}
      />
      <ShopProductGrid params={{ audience, occasion: slug, sort: "createdAt:desc" }} />
    </Container>
  );
}
