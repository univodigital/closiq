"use client";

import { use } from "react";
import { useQuery } from "@tanstack/react-query";
import { categoryService } from "@/features/products/services";
import { useListingParams } from "@/features/products/hooks/useListingParams";
import { ListingDateBar } from "@/features/products/components/ListingDateBar";
import { WishlistProductCard } from "@/features/wishlist/components/WishlistProductCard";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ProductCardSkeleton } from "@/components/ui/skeleton";

export default function OccasionPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = use(params);
  const listingParams = useListingParams();
  const categories = useQuery({ queryKey: ["categories"], queryFn: () => categoryService.listCategories() });
  const products = useQuery({
    queryKey: ["category", slug, listingParams],
    queryFn: () => categoryService.getCategoryProducts(slug, listingParams),
    staleTime: 60_000,
    refetchOnWindowFocus: true,
  });

  const cat = categories.data?.data.find((c) => c.slug === slug);

  function scrollToDateBar() {
    document.getElementById("listing-date-bar")?.scrollIntoView({ behavior: "smooth", block: "center" });
  }

  return (
    <Container className="py-10 md:py-14">
      <PageHeader
        title={cat?.name ?? slug}
        description={cat?.description}
        breadcrumb="Occasion"
      />
      <ListingDateBar className="mb-8 max-w-md rounded-sm border border-border p-4" />
      <div className="grid grid-cols-2 gap-5 md:grid-cols-3 lg:grid-cols-4 md:gap-7">
        {products.isLoading
          ? Array.from({ length: 4 }).map((_, i) => <ProductCardSkeleton key={i} />)
          : products.data?.data.map((p) => (
              <WishlistProductCard key={p.id} product={p} onChangeDates={scrollToDateBar} />
            ))}
      </div>
    </Container>
  );
}
