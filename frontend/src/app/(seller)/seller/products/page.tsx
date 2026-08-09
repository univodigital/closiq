"use client";

import Link from "next/link";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { sellerService } from "@/features/seller/services";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";

export default function SellerProductsPage() {
  const { data, isLoading } = useQuery({
    queryKey: ["seller", "products"],
    queryFn: () => sellerService.listProducts(),
  });

  return (
    <div>
      <div className="mb-8 flex items-end justify-between">
        <PageHeader title="Listings" />
        <Button asChild variant="primary" size="sm">
          <Link href={ROUTES.seller.productNew}>New listing</Link>
        </Button>
      </div>
      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : (
        <div className="space-y-3">
          {data?.data.map((listing) => (
            <Link
              key={listing.id}
              href={ROUTES.seller.product(listing.id)}
              className="flex items-center gap-4 rounded-sm border border-border p-4 hover:bg-muted/30"
            >
              <div className="relative h-16 w-12 shrink-0 overflow-hidden rounded-sm bg-muted">
                <Image
                  src={listing.imageUrl ?? "/placeholder-product.jpg"}
                  alt=""
                  fill
                  className="object-cover"
                  sizes="48px"
                />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-medium">{listing.title}</p>
                  <StatusBadge status={listing.status.toLowerCase()} />
                </div>
                <p className="text-sm text-muted-foreground">
                  {formatCurrency(listing.pricePerDay)}/day
                </p>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
