"use client";

import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ShopProductGrid } from "@/features/shop/components/ShopProductGrid";
import { SHOP_AUDIENCE_LABELS, type ShopAudienceSlug } from "@/shared/constants/shop-nav";

export function ShopDiscoverView({
  audience,
  slug,
}: {
  audience: ShopAudienceSlug;
  slug: "new-in" | "trending";
}) {
  const audienceLabel = SHOP_AUDIENCE_LABELS[audience];
  const title = slug === "trending" ? "Trending" : "New In";
  const description =
    slug === "trending"
      ? `Popular ${audienceLabel.toLowerCase()} pieces right now.`
      : `Latest ${audienceLabel.toLowerCase()} drops this week.`;

  return (
    <Container className="py-10 md:py-14">
      <PageHeader
        title={`${audienceLabel} · ${title}`}
        description={description}
        breadcrumb={audienceLabel}
      />
      <ShopProductGrid
        params={{
          audience,
          trending: slug === "trending" ? true : undefined,
          occasion: slug === "new-in" ? "new-in" : undefined,
          sort: "createdAt:desc",
        }}
      />
    </Container>
  );
}
