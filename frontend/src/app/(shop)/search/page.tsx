"use client";

import { useSearchParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { productService } from "@/features/products/services";
import { WishlistProductCard } from "@/features/wishlist/components/WishlistProductCard";
import { Container, PageHeader } from "@/shared/components/layout/Container";
import { Input } from "@/components/ui/input";
import { ProductCardSkeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/shared/components/feedback/EmptyState";
import { Search } from "lucide-react";
import { useState } from "react";
import { ROUTES } from "@/shared/constants/routes";

export default function SearchPage() {
  const searchParams = useSearchParams();
  const initialQ = searchParams.get("q") ?? "";
  const [q, setQ] = useState(initialQ);

  const { data, isLoading } = useQuery({
    queryKey: ["search", q],
    queryFn: () => productService.searchProducts(q),
    enabled: q.length >= 2,
  });

  return (
    <Container className="py-10 md:py-14">
      <PageHeader title="Search" />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          const fd = new FormData(e.currentTarget);
          setQ(String(fd.get("q") ?? ""));
        }}
        className="mb-10"
      >
        <Input name="q" defaultValue={initialQ} placeholder="Saree, gown, designer…" aria-label="Search products" />
      </form>
      {q.length < 2 ? (
        <p className="text-sm text-muted-foreground">Type at least 2 characters to search.</p>
      ) : isLoading ? (
        <div className="grid grid-cols-2 gap-5 md:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <ProductCardSkeleton key={i} />
          ))}
        </div>
      ) : data?.data.length === 0 ? (
        <EmptyState icon={Search} title="No results" description={`Nothing matched "${q}". Try another search.`} actionLabel="Browse all" actionHref={ROUTES.products} />
      ) : (
        <div className="grid grid-cols-2 gap-5 md:grid-cols-4 md:gap-7">
          {data?.data.map((p) => <WishlistProductCard key={p.id} product={p} />)}
        </div>
      )}
    </Container>
  );
}
