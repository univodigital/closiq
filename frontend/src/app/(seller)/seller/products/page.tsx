"use client";

import Link from "next/link";
import Image from "next/image";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { sellerService } from "@/features/seller/services";
import { ListingActionsMenu } from "@/features/seller/components/ListingActionsMenu";
import { PageHeader } from "@/shared/components/layout/Container";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/badge";
import { formatCurrency } from "@/lib/format";
import { ROUTES } from "@/shared/constants/routes";
import { cn } from "@/lib/utils";

type ListingFilter = "ALL" | "ACTIVE" | "DRAFT" | "ARCHIVED";

const FILTERS: Array<{ value: ListingFilter; label: string }> = [
  { value: "ALL", label: "All" },
  { value: "ACTIVE", label: "Live" },
  { value: "DRAFT", label: "Drafts" },
  { value: "ARCHIVED", label: "Archived" },
];

export default function SellerProductsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<ListingFilter>("ALL");

  const { data, isLoading } = useQuery({
    queryKey: ["seller", "products", filter],
    queryFn: () =>
      sellerService.listProducts(filter === "ALL" ? undefined : { status: filter }),
  });

  function refreshListings() {
    void queryClient.invalidateQueries({ queryKey: ["seller", "products"] });
  }

  const listings = data?.data ?? [];
  const emptyMessage =
    filter === "ARCHIVED"
      ? "No archived listings yet."
      : filter === "DRAFT"
        ? "No draft listings yet."
        : filter === "ACTIVE"
          ? "No live listings yet."
          : "No listings yet.";

  return (
    <div>
      <div className="mb-8 flex items-end justify-between gap-3">
        <PageHeader title="Listings" />
        <div className="flex gap-2">
          <Button asChild variant="outline" size="sm">
            <Link href={ROUTES.seller.productBulk}>Bulk upload</Link>
          </Button>
          <Button asChild variant="primary" size="sm">
            <Link href={ROUTES.seller.productNew}>New listing</Link>
          </Button>
        </div>
      </div>

      <div className="mb-6 flex flex-wrap gap-2">
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            onClick={() => setFilter(value)}
            className={cn(
              "rounded-sm border px-3 py-1.5 text-sm transition-colors",
              filter === value
                ? "border-foreground bg-foreground text-background"
                : "border-border text-muted-foreground hover:border-foreground/30 hover:text-foreground",
            )}
          >
            {label}
          </button>
        ))}
      </div>

      {isLoading ? (
        <p className="text-muted-foreground">Loading…</p>
      ) : listings.length === 0 ? (
        <p className="text-muted-foreground">{emptyMessage}</p>
      ) : (
        <div className="space-y-3">
          {listings.map((listing) => (
            <div
              key={listing.id}
              className="flex items-center gap-3 rounded-sm border border-border p-4 transition-colors hover:bg-muted/20"
            >
              <Link
                href={ROUTES.seller.product(listing.id)}
                className="flex min-w-0 flex-1 items-center gap-4"
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
              <ListingActionsMenu listing={listing} onUpdated={refreshListings} />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
