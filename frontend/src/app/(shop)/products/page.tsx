"use client";

import { useQuery } from "@tanstack/react-query";
import { productService } from "@/features/products/services";
import { WishlistProductCard } from "@/features/wishlist/components/WishlistProductCard";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/shared/components/feedback/EmptyState";

export default function ProductsPage() {
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["products"],
    queryFn: () => productService.listProducts({ sort: "createdAt:desc" }),
  });

  if (isError) return <ErrorState onRetry={() => refetch()} />;

  return (
    <Container className="py-10 md:py-14">
      <PageHeader title="All pieces" description="Premium rentals with 15-minute home trial included." />
      <div className="grid grid-cols-2 gap-5 md:grid-cols-3 lg:grid-cols-4 md:gap-7">
        {isLoading
          ? Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)
          : data?.data.map((p) => <WishlistProductCard key={p.id} product={p} />)}
      </div>
    </Container>
  );
}
